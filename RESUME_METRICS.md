# Verified Resume Metrics

Generated from `benchmarks/results/**/*.json` by `python benchmarks/generate_report.py`.

- Improved authenticated problem-library throughput by 292.75% (68.627 → 269.534 requests/s) at 8 concurrent local clients by replacing per-problem submission loading with grouped aggregation.
- Reduced problem-library p95 latency by 64.21% (142.250 ms → 50.915 ms) at 8 concurrent local clients on the reproducible H2 workload.
- Sustained 269.534 requests/s with 0.0000% request errors at the highest tested level of 8 concurrent local clients after the query optimization.
