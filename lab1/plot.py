#!/usr/bin/env python3
"""Build the summary throughput chart (M ops/s vs thread count) from measure.log."""
import re
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

LOG = sys.argv[1] if len(sys.argv) > 1 else "/tmp/opencode/measure.log"
OUT = sys.argv[2] if len(sys.argv) > 2 else "throughput.png"

THREADS = [1, 2, 4, 8, 16]

LABELS = {
    "empty-lock": "Этап 1: пустой лок",
    "sync": "Этап 1: единый общий лок",
    "striped": "Этап 2: шардированный лок",
    "thread-local": "Этап 3: thread-local",
    "double-buffer": "Этап 4: двойная буферизация",
}

data = {name: {} for name in LABELS}
baseline = None

pattern = re.compile(r"RESULT (\S+) T=(\d+):\s+([\d.]+) M ops/s")
with open(LOG, encoding="utf-8") as handle:
    for line in handle:
        match = pattern.search(line)
        if not match:
            continue
        name, threads, value = match.group(1), int(match.group(2)), float(match.group(3))
        if name == "one-thread":
            baseline = value
        elif name in data:
            data[name][threads] = value

plt.figure(figsize=(9, 5.5))
markers = ["o", "s", "^", "D", "v"]
for (name, label), marker in zip(LABELS.items(), markers):
    points = [(t, data[name][t]) for t in THREADS if t in data[name]]
    if not points:
        continue
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    plt.plot(xs, ys, marker=marker, linewidth=1.8, label=label)

if baseline is not None:
    plt.axhline(baseline, linestyle="--", color="gray", linewidth=1.4,
                label=f"Этап 0: однопоточный baseline ({baseline:.0f})")

plt.xscale("log", base=2)
plt.xticks(THREADS, [str(t) for t in THREADS])
plt.xlabel("Число потоков (T)")
plt.ylabel("Пропускная способность, млн оп/сек")
plt.title("Коллектор метрик: пропускная способность от числа потоков")
plt.grid(True, which="both", linestyle=":", alpha=0.5)
plt.legend()
plt.tight_layout()
plt.savefig(OUT, dpi=150)
print(f"saved {OUT}")
