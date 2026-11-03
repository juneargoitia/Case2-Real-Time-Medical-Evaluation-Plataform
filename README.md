# Case 2: Real-Time Medical Evaluation Platform

Study case for *Monitoring and Performance Engineering*. The repository contains:

- **`java/`**: Java 17+ program (package `monitoring`) with
  - `EarlyBindingAverage`: incremental sliding-window average (early binding),
  - `LateBindingAverage`: baseline that recomputes from stored history,
  - `Watchdog`: raises `DEVICE_OFFLINE` when a device goes silent,
  - `Benchmark`: late vs early binding timings, writes `results/benchmark.csv`,
  - `FailureSimulation`: replays the incident, writes `results/simulation.csv` and `results/alerts.csv`.
- **`python/make_figures.py`**: reads `results/*.csv` and generates the figures and LaTeX tables of the report.
- **`results/`**: CSV output of the Java program (committed so the report is reproducible).
- **`report/`**: Overleaf-ready LaTeX report (`main.tex`, `references.bib`, figures, tables, code listings).
- **`scripts/build_all.sh`**: rebuilds everything.

## Run it

Requirements: JDK 17+, Python 3.9+ (`pip install -r python/requirements.txt`).

```bash
bash scripts/build_all.sh        # compile + run Java, make figures, copy listings
bash scripts/build_all.sh pdf    # ...and compile the report (needs latexmk)
```

Without the script (e.g. on Windows):

```bash
javac -d java/out java/src/monitoring/*.java
java -cp java/out monitoring.Main results
python python/make_figures.py
```

Benchmark timings vary with the machine; rerun the steps above to regenerate figures and numbers in the report.

## Report in Overleaf

Upload the **`report/`** folder as a new project (zip it, then *New Project > Upload Project*). Everything the report needs is inside it. Set `main.tex` as the main file and compile with pdfLaTeX. Replace the author, university and repository URL placeholders in `main.tex`.
