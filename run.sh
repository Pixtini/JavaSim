#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$project_root/build.sh"

main_class=${1:-ToolkitGUI}
if [ "$#" -gt 0 ]; then
    shift
fi
exec java -cp "$project_root/build/classes" "$main_class" "$@"
