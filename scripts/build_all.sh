#!/usr/bin/env bash
# Rebuilds everything: Java run -> CSV results -> figures/tables -> report listings (-> PDF).
# Usage: bash scripts/build_all.sh          (add "pdf" to also compile the report)
set -euo pipefail
cd "$(dirname "$0")/.."

echo "== 1/4 Compile Java"
rm -rf java/out
javac -d java/out java/src/monitoring/*.java

echo "== 2/4 Run Java (simulation + benchmark)"
java -cp java/out monitoring.Main results

echo "== 3/4 Python: figures and tables"
python3 python/make_figures.py

echo "== 4/4 Copy source listings into the report folder (so Overleaf needs only report/)"
cp java/src/monitoring/*.java report/listings/

if [[ "${1:-}" == "pdf" ]]; then
  echo "== Compile report"
  (cd report && latexmk -pdf -interaction=nonstopmode main.tex)
fi
echo "Done."
