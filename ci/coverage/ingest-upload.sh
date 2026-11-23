#!/usr/bin/env bash
# Stores a coverage upload pushed by a GitHub Action (see .github/actions/upload-coverage) in the staging folder
# of the combined coverage job, as <staging>/<repo>/, replacing the previous upload of that repo. The Jenkins jobs
# write to the same folder themselves (pushCoverage in their Jenkinsfile), so the combined report always holds the
# LAST coverage of every repo.
#
# Usage: bash ci/coverage/ingest-upload.sh <zip file> <repo name> <staging folder>
set -euo pipefail

zip="${1:?usage: ingest-upload.sh <zip file> <repo name> <staging folder>}"
repo="${2:?usage: ingest-upload.sh <zip file> <repo name> <staging folder>}"
staging="${3:?usage: ingest-upload.sh <zip file> <repo name> <staging folder>}"

# the repo name becomes a directory name: no slashes, no leading dot (so no '..')
if ! [[ "$repo" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$ ]]; then
    echo "invalid repo name: $repo" >&2
    exit 2
fi

# Names the Jenkins jobs push with pushCoverage are reserved: an upload (anyone with the trigger token)
# must not be able to overwrite the coverage of a Jenkins-built repo. Add a name here when a new Jenkins
# job starts calling pushCoverage.
case "$repo" in
    servoy-eclipse|server|servoy-client|servoy-extensions|servoy-copilot|sablo)
        echo "'$repo' is pushed by its Jenkins job, a GitHub upload cannot use that name" >&2
        exit 2
        ;;
esac

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

mkdir -p "$tmp/unpacked"
if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$zip" -d "$tmp/unpacked"   # unzip refuses entries that climb out of the target dir
else
    abs="$(cd "$(dirname "$zip")" && pwd)/$(basename "$zip")"
    (cd "$tmp/unpacked" && jar xf "$abs")
fi

mkdir -p "$staging"
new="$staging/$repo.new.$$"
rm -rf "$new"
mkdir -p "$new"
count=0
# only coverage data is kept, whatever else was in the zip is dropped
while IFS= read -r f; do
    rel="${f#"$tmp/unpacked"/}"
    mkdir -p "$new/$(dirname "$rel")"
    cp "$f" "$new/$rel"
    count=$((count + 1))
done < <(find "$tmp/unpacked" -type f \( -name '*.xml' -o -name 'lcov.info' \))

if [ "$count" -eq 0 ]; then
    rm -rf "$new"
    echo "the upload for $repo holds no .xml or lcov.info files" >&2
    exit 1
fi

# swap in the new upload; the old one is removed only after the new one is in place
rm -rf "$staging/$repo.old"
if [ -d "$staging/$repo" ]; then mv "$staging/$repo" "$staging/$repo.old"; fi
mv "$new" "$staging/$repo"
rm -rf "$staging/$repo.old"
echo "stored $count coverage file(s) for $repo in $staging/$repo"
