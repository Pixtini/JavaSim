#!/bin/sh
# Reproduce G002 checks without changing any mathematical input or existing game config.
set -eu
cd "$(dirname "$0")/../../.."
stage=${1:-smoke}
case "$stage" in smoke|balance|all) ;; *) echo 'Expected smoke, balance or all' >&2; exit 2 ;; esac
./build.sh
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeGameTests
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeMathAudit game/G002-ember-forge/validation/exact-math.csv
runner_dir=$(mktemp -d "${TMPDIR:-/tmp}/g002-validation.XXXXXX")
trap 'rm -rf "$runner_dir"' EXIT HUP INT TERM
cp .agents/skills/javasim-validate-math/assets/SeededSimulation.java.template "$runner_dir/SeededSimulation.java"
javac -cp build/classes -d "$runner_dir" "$runner_dir/SeededSimulation.java"
for pass in smoke balance; do
    if [ "$stage" != all ] && [ "$stage" != "$pass" ]; then continue; fi
    seed=440055
    for mode in Normal EM1 BB1; do
        rounds=2000
        if [ "$pass" = balance ]; then
            rounds=2000000
            if [ "$mode" = BB1 ]; then rounds=200000; fi
        fi
        java -Xmx512m -cp "build/classes:$runner_dir" SeededSimulation \
            G002-ember-forge game/G002-ember-forge/source/PAR.xlsx "$mode" \
            "$rounds" 1.0 "$seed" 2 16 "game/G002-ember-forge/validation/runs/$mode-$pass" 10
        seed=$((seed + 1))
    done
done
python3 game/G002-ember-forge/validation/verify_reports.py
