#!/usr/bin/env bash
# Stores a coverage upload pushed by a GitHub Action (see .github/actions/upload-coverage) in
# github-uploads/<repo>/, replacing the previous upload of that repo. The next aggregation run
# (aggregate.sh) picks it up, so the combined report always holds the LAST upload of every repo.
#
# Usage: bash ci/coverage/ingest-upload.sh <zip file> <repo name>
set -euo pipefail

zip="${1:?usage: ingest-upload.sh <zip file> <repo name>}"
repo="${2:?usage: ingest-upload.sh <zip file> <repo name>}"

# the repo name becomes a directory name: no slashes, no leading dot (so no '..')
if ! [[ "$repo" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$ ]]; then
    echo "invalid repo name: $repo" >&2
    exit 2
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$zip" -d "$tmp"            # unzip refuses entries that climb out of the target dir
else
    abs="$(cd "$(dirname "$zip")" && pwd)/$(basename "$zip")"
    (cd "$tmp" && jar xf "$abs")
fi

mkdir -p github-uploads
stage="$(mktemp -d "github-uploads/.incoming.XXXXXX")"
count=0
# only coverage data is kept, whatever else was in the zip is dropped
while IFS= read -r f; do
    rel="${f#"$tmp"/}"
    mkdir -p "$stage/$(dirname "$rel")"
    cp "$f" "$stage/$rel"
    count=$((count + 1))
done < <(find "$tmp" -type f \( -name '*.xml' -o -name 'lcov.info' \) ! -type l)

if [ "$count" -eq 0 ]; then
    rm -rf "$stage"
    echo "the upload for $repo holds no .xml or lcov.info files" >&2
    exit 1
fi

rm -rf "github-uploads/$repo"
mv "$stage" "github-uploads/$repo"
echo "stored $count coverage files for $repo in github-uploads/$repo"
