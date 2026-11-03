from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.patches import FancyArrowPatch, FancyBboxPatch

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "results"
FIGS = ROOT / "report" / "figures"
TABLES = ROOT / "report" / "tables"

NAVY, BLUE, RED, GREY, GREEN, PURPLE = (
    "#1F4E79", "#2E75B6", "#C0392B", "#7F7F7F", "#2E8B57", "#8E44AD")

plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})


def _box(ax, x, y, w, h, text, fc, fs=9):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.08",
                                fc=fc, ec="none"))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center",
            color="white", fontsize=fs, fontweight="bold")


def _arrow(ax, x1, y1, x2, y2, color=GREY):
    ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="-|>",
                                 mutation_scale=16, lw=1.8, color=color))


def fig_pipeline():
    fig, ax = plt.subplots(figsize=(11, 5))
    ax.set_xlim(0, 11)
    ax.set_ylim(0.3, 5.2)
    ax.axis("off")

    _box(ax, 0.2, 2.2, 1.5, 1.1, "Wearable\nsensors", NAVY, 10)
    _box(ax, 2.2, 2.2, 1.6, 1.1, "Edge gateway\n(filter noise,\nheartbeat)", BLUE)
    _box(ax, 4.3, 2.2, 1.5, 1.1, "Message broker\n(partition by\npatient ID)", BLUE)
    _arrow(ax, 1.7, 2.75, 2.2, 2.75)
    _arrow(ax, 3.8, 2.75, 4.3, 2.75)

    ax.text(6.2, 4.85, "FAST PATH (early binding, light work only)", color=RED,
            fontsize=10, fontweight="bold")
    _box(ax, 6.2, 3.7, 1.9, 0.95, "Stream processor\nincremental windows", RED)
    _box(ax, 8.4, 3.7, 1.2, 0.95, "Alert\nengine", RED)
    _box(ax, 9.8, 3.7, 1.0, 0.95, "Notify\nstaff /\nrelatives", RED, 8)
    _arrow(ax, 5.8, 2.95, 6.2, 4.1, RED)
    _arrow(ax, 8.1, 4.17, 8.4, 4.17, RED)
    _arrow(ax, 9.6, 4.17, 9.8, 4.17, RED)

    _box(ax, 7.0, 2.35, 2.6, 0.8, "In-memory store (Redis)\nlatest state + status flag", GREEN)
    _arrow(ax, 7.15, 3.7, 7.6, 3.15, GREEN)
    _arrow(ax, 8.9, 3.15, 8.95, 3.7, GREEN)
    _box(ax, 9.8, 2.35, 1.0, 0.8, "Watchdog\nno data = alert", PURPLE, 8)
    _arrow(ax, 10.3, 3.15, 10.3, 3.7, PURPLE)

    ax.text(6.2, 1.55, "SLOW PATH (late binding, analytics)", color=GREY,
            fontsize=10, fontweight="bold")
    _box(ax, 6.2, 0.5, 1.9, 0.95, "Time-series DB\n(aggregates)", GREY)
    _box(ax, 8.4, 0.5, 2.4, 0.95, "Cold storage\nreports, audits, trends", GREY)
    _arrow(ax, 5.8, 2.45, 6.2, 1.1, GREY)
    _arrow(ax, 8.1, 0.97, 8.4, 0.97, GREY)

    fig.savefig(FIGS / "fig_pipeline.png", dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def fig_benchmark(df):
    late_us = df["late_ms"] * 1e3
    early_us = df["early_ms"] * 1e3
    fig, ax = plt.subplots(figsize=(7.5, 4.4))
    ax.plot(df["readings"], late_us, "o-", color=RED, lw=2.2,
            label="Late binding: recompute from stored history")
    ax.plot(df["readings"], early_us, "o-", color=GREEN, lw=2.2,
            label="Early binding: incremental window")
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("Readings already stored for the patient")
    ax.set_ylabel("Time per request (\u00b5s, log scale)")
    ax.grid(True, which="both", alpha=0.25)
    ax.legend(frameon=False, loc="center right")
    fig.savefig(FIGS / "fig_benchmark.png", dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def grp(n):
    return f"{int(n):,}".replace(",", "\\,")


def sig2(x):
    return grp(float(f"{x:.2g}"))


def benchmark_table(df):
    rows = []
    for _, r in df.iterrows():
        late_us = r.late_ms * 1e3
        early_ns = r.early_ms * 1e6
        speed = r.late_ms / r.early_ms
        cores = r.late_ms
        late_txt = f"{late_us:,.1f}".replace(",", "\\,")
        rows.append(f"{grp(r.readings)} & {late_txt} & {early_ns:.1f} & "
                    f"{sig2(speed)} & {cores:.3f} \\\\")
    body = "\n".join(rows)
    tex = (
        "\\begin{tabular}{rrrrr}\n\\toprule\n"
        "Stored readings & Late binding (\\textmu s) & Early binding (ns) & "
        "Speed-up ($\\times$) & Cores needed (late)$^{*}$ \\\\\n\\midrule\n"
        f"{body}\n\\bottomrule\n\\end{{tabular}}\n"
    )
    (TABLES / "benchmark_table.tex").write_text(tex, encoding="utf-8")


def fig_watchdog(sim, alerts, fail_t):
    fig, axes = plt.subplots(3, 1, figsize=(8.5, 6.6), sharex=True)
    titles = ["Patient 1: normal", "Patient 2: tachycardia episode",
              "Patient 3: device fails silently"]
    for p, ax in enumerate(axes, start=1):
        d = sim[sim.patient == p]
        ax.plot(d.time_s, d.heart_rate, color=NAVY, lw=1.1)
        ax.axhline(120, color=GREY, ls="--", lw=1)
        ax.set_ylabel("bpm")
        ax.set_ylim(50, 150)
        ax.set_title(titles[p - 1], loc="left", fontsize=10, fontweight="bold")
        for _, a in alerts[alerts.patient == p].iterrows():
            colour = RED if a.type == "THRESHOLD" else PURPLE
            ax.axvline(a.time_s, color=colour, lw=2)
            x_text = a.time_s + 62 if a.type == "THRESHOLD" else a.time_s + 4
            ax.text(x_text, 138, f"{a.type}\nt = {a.time_s} s", color=colour,
                    fontsize=9, fontweight="bold", va="center")
    ax3 = axes[2]
    ax3.axvspan(fail_t, sim.time_s.max(), color="#EEEEEE")
    ax3.text((fail_t + sim.time_s.max()) / 2, 95,
             "no data arrives\n(without a watchdog, no alert is ever raised)",
             ha="center", fontsize=9, color=GREY)
    ax3.set_xlabel("Time (s)")
    axes[0].text(598, 122, "threshold (5 s average)", ha="right", va="bottom",
                 fontsize=8, color=GREY)
    fig.tight_layout()
    fig.savefig(FIGS / "fig_watchdog.png", dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def numbers(df, sim, alerts):
    env = dict(line.split("=", 1) for line in
               (RESULTS / "environment.txt").read_text().split())
    first, last = df.iloc[0], df.iloc[-1]
    tachy = alerts[alerts.type == "THRESHOLD"].iloc[0]
    off = alerts[alerts.type == "DEVICE_OFFLINE"].iloc[0]
    fail_t = int(sim[(sim.patient == 3) & sim.heart_rate.isna()].time_s.min())
    early_ns = df.early_ms * 1e6
    macros = {
        "nSmall": grp(first.readings),
        "nLarge": grp(last.readings),
        "lateSmall": f"{first.late_ms * 1e3:.1f}\\,\\textmu s",
        "lateLarge": f"{last.late_ms:.2f}\\,ms",
        "earlyLow": f"{early_ns.min():.1f}",
        "earlyHigh": f"{early_ns.max():.1f}",
        "speedupLarge": sig2(last.late_ms / last.early_ms),
        "failTime": str(fail_t),
        "offlineTime": str(int(off.time_s)),
        "offlineDelay": str(int(off.time_s) - fail_t),
        "tachyTime": str(int(tachy.time_s)),
        "javaVersion": env["java"],
        "cpuCores": env["cores"],
    }
    lines = [f"\\newcommand{{\\{k}}}{{{v}}}" for k, v in macros.items()]
    (TABLES / "numbers.tex").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    FIGS.mkdir(parents=True, exist_ok=True)
    TABLES.mkdir(parents=True, exist_ok=True)
    bench = pd.read_csv(RESULTS / "benchmark.csv")
    sim = pd.read_csv(RESULTS / "simulation.csv")
    alerts = pd.read_csv(RESULTS / "alerts.csv")
    fail_t = int(sim[(sim.patient == 3) & sim.heart_rate.isna()].time_s.min())

    fig_pipeline()
    fig_benchmark(bench)
    fig_watchdog(sim, alerts, fail_t)
    benchmark_table(bench)
    numbers(bench, sim, alerts)
    print("Figures and tables written to", FIGS, "and", TABLES)


if __name__ == "__main__":
    main()
