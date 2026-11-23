#!/usr/bin/env bash
# Combines every coverage report found under
#   incoming/<source>/<path in that repo>   (a copy of the staging folder the repos push to,
#                                             see Jenkinsfile.coverage)
# into ONE ReportGenerator report (https://github.com/danielpalme/ReportGenerator).
#
# Output (relative to the working directory):
#   report-html/   browsable HTML (index.html), badges
#   report-data/   Cobertura.xml, Summary.json/.csv/.txt, SummaryGithub.md, sources.txt
#   history/       ReportGenerator history (coverage trend charts), kept between runs
#
# Every input file becomes ONE row ("assembly") in the report, named "<source>/<bundle> (Java|JS)",
# so the overview is per repo and bundle instead of per Java package. This is done by renaming all
# <package name="..."> of a JaCoCo/Cobertura file to that label before ReportGenerator reads it.
#
# Needs: bash, curl, sha256sum, sed, grep, find, unzip or jar (JDK). .NET 10 runtime: used when
# present, otherwise installed without root into .tools/ (needs internet, like the node download).
# Env: COVERAGE_WORKDIR (default .), COVERAGE_TOOLS (default .tools), COVERAGE_TITLE, BUILD_NUMBER.
set -euo pipefail
export MSYS_NO_PATHCONV=1   # git-bash on Windows must not rewrite the -reports:... arguments

cd "${COVERAGE_WORKDIR:-.}"
TOOLS="${COVERAGE_TOOLS:-.tools}"
RG_VERSION=5.6.1
RG_SHA256=941ac602bc6be7f92bf045c101f05403968f00f2ed948041bbd76c9f5707469f   # nupkg of ReportGenerator 5.6.1
RG_DLL="$TOOLS/reportgenerator-$RG_VERSION/tools/net10.0/ReportGenerator.dll"

export DOTNET_NOLOGO=1 DOTNET_CLI_TELEMETRY_OPTOUT=1
export DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=1   # no libicu needed on the agent

extract_zip() { # $1 zip, $2 target dir
    mkdir -p "$2"
    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "$1" -d "$2"
    else
        local abs; abs="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
        (cd "$2" && jar xf "$abs")
    fi
}

# ---- tools -------------------------------------------------------------------------------------
mkdir -p "$TOOLS"
if command -v dotnet >/dev/null 2>&1 && dotnet --list-runtimes 2>/dev/null | grep -q '^Microsoft.NETCore.App 10\.'; then
    DOTNET=dotnet
else
    if [ ! -x "$TOOLS/dotnet/dotnet" ]; then
        echo "installing the .NET 10 runtime into $TOOLS/dotnet"
        curl -fsSL https://dot.net/v1/dotnet-install.sh -o "$TOOLS/dotnet-install.sh"
        bash "$TOOLS/dotnet-install.sh" --runtime dotnet --channel 10.0 --install-dir "$TOOLS/dotnet" --no-path
    fi
    export DOTNET_ROOT="$PWD/$TOOLS/dotnet"
    DOTNET="$PWD/$TOOLS/dotnet/dotnet"
fi

if [ ! -f "$RG_DLL" ]; then
    echo "downloading ReportGenerator $RG_VERSION"
    curl -fsSL -o "$TOOLS/reportgenerator.nupkg" "https://www.nuget.org/api/v2/package/ReportGenerator/$RG_VERSION"
    echo "$RG_SHA256  $TOOLS/reportgenerator.nupkg" | sha256sum -c - >/dev/null || { echo "ReportGenerator checksum mismatch" >&2; exit 1; }
    rm -rf "$TOOLS/reportgenerator-$RG_VERSION"
    extract_zip "$TOOLS/reportgenerator.nupkg" "$TOOLS/reportgenerator-$RG_VERSION"
fi

# ---- collect the inputs ------------------------------------------------------------------------
kind_of() { # prints jacoco | cobertura | none, from the first bytes of the file
    local head; head="$(head -c 4096 "$1" | tr -d '\r')"
    if grep -q -e '<!DOCTYPE report' -e '<report name=' <<<"$head"; then echo jacoco
    elif grep -q '<coverage' <<<"$head"; then echo cobertura
    else echo none; fi
}

rm -rf prepared report-html report-data
mkdir -p prepared report-data history
n=0
: > report-data/sources.txt

emit() { # $1 file, $2 label, $3 java|js
    local label="${2//[^A-Za-z0-9._\/ ()-]/_}"
    n=$((n + 1))
    # one assembly per input file: rename every package to the label (class names stay fully qualified)
    sed -E "s#<package name=\"[^\"]*\"#<package name=\"${label}\"#g" "$1" > "$(printf 'prepared/%04d.xml' "$n")"
    printf '%s\t%s\n' "$label" "$1" >> report-data/sources.txt
}

# incoming/<source>/<path in that repo>: label = <source>/<module>, where the module is the folder before
# target/ (Maven/Tycho: <bundle>/target/site/jacoco/jacoco.xml) or before coverage/ (Angular: <x>/coverage/combined/...)
while IFS= read -r f; do
    rel="${f#incoming/}"; src="${rel%%/*}"; rest="${rel#*/}"
    dir="$(dirname "$rest")"
    dir="${dir%%/target*}"                                  # <module>/target/site/jacoco -> <module>
    dir="$(sed -E 's#(^|/)coverage(/[^/]+)?$##' <<<"$dir")" # <x>/coverage/combined -> <x>
    case "$dir" in .|target|target/*) dir="" ;; esac
    dir="${dir##*/}"                                        # bundles/com.x.y -> com.x.y
    label="$src${dir:+/$dir}"
    case "$(kind_of "$f")" in
        jacoco)    emit "$f" "$label (Java)" ;;
        cobertura) emit "$f" "$label (JS)" ;;
        *)         echo "WARN: not a JaCoCo/Cobertura file, skipped: $f" >&2 ;;
    esac
done < <(find incoming -type f -name '*.xml' 2>/dev/null | sort)

# lcov has no package names to relabel: all lcov files end up in the default assembly
l=0
while IFS= read -r f; do
    l=$((l + 1)); mkdir -p "prepared/lcov-$l"; cp "$f" "prepared/lcov-$l/lcov.info"
    printf '(lcov, not relabelled)\t%s\n' "$f" >> report-data/sources.txt
done < <(find incoming -type f -name 'lcov.info' 2>/dev/null | sort)

if [ "$n" -eq 0 ] && [ "$l" -eq 0 ]; then
    echo "no coverage files found under incoming/ (nothing pushed to the staging folder yet?)" >&2
    exit 1
fi
echo "combining $n JaCoCo/Cobertura files and $l lcov files"

# ---- ReportGenerator ---------------------------------------------------------------------------
common=("-reports:prepared/*.xml;prepared/lcov-*/lcov.info"
        "-title:${COVERAGE_TITLE:-Servoy combined coverage}"
        "-tag:${BUILD_NUMBER:-local}"
        "-verbosity:Error")

run_rg() { # $1 report types, $2 target dir, $3... extra args
    local types="$1" target="$2"; shift 2
    if ! "$DOTNET" "$RG_DLL" "${common[@]}" "-reporttypes:$types" "-targetdir:$target" "$@" > "rg-$target.log" 2>&1; then
        tail -n 40 "rg-$target.log" >&2
        echo "ReportGenerator failed ($types)" >&2
        exit 1
    fi
    # without source folders ReportGenerator logs one line per file: keep the log quiet
    { grep -v -e 'does not exist (any more)' "rg-$target.log" || true; } | tail -n 10
}

run_rg "Html;Badges" report-html "-historydir:history"
run_rg "Cobertura;JsonSummary;TextSummary;MarkdownSummaryGithub;CsvSummary" report-data

echo
sed -n '1,14p' report-data/Summary.txt
