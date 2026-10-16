#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
classes_dir="$project_root/build/classes"
mkdir -p "$classes_dir"
find "$project_root" -type f -name '*.java' \
    -not -path "$project_root/.git/*" \
    -not -path "$project_root/build/*" \
    -print0 | xargs -0 javac -d "$classes_dir"

printf 'Compiled Java sources to %s\n' "$classes_dir"
