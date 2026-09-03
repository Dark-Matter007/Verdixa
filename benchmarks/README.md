# Verdixa Benchmarks

This suite measures the authenticated problem-library read path with a deterministic, local-only Spring profile. It is designed for reproducibility, not as a substitute for MySQL or distributed production testing.

## Prerequisites

- Java 21 and Maven 3.9+
- Python 3.11+ (standard library only)
- A writable Maven cache. In restricted shells, set `MAVEN_OPTS=-Duser.home=<repository-root>`.

## Workload

The `benchmark` profile starts H2 in MySQL-compatibility mode, creates its schema, and seeds one login user (`benchmark-user` / `benchmark-password`), 100 active problems, and 1,000 submissions. The target is `GET /api/problems/library?size=100` using a fresh JWT obtained from the seeded account.

The default levels are 1, 4, and 8 concurrent clients. Every level uses a 10-second warm-up and 30-second measurement window unless overridden. The load client writes all request latencies plus summary percentiles to JSON.

## Commands

From the repository root in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File benchmarks/run_local.ps1 -Label baseline
# make and verify the implementation change
powershell -ExecutionPolicy Bypass -File benchmarks/run_local.ps1 -Label optimized
python benchmarks/generate_report.py
```

`run_local.ps1` uses port 18080 by default and refuses to run if that port is already in use. Use `-Port 18081` if needed. It records environment, startup, resource snapshots, and one raw JSON file per concurrency level under `benchmarks/results/<label>/`.

For a running benchmark server, run the non-destructive request checks:

```powershell
python benchmarks/reliability_checks.py --base-url http://127.0.0.1:18080 --output benchmarks/results/reliability.json
```

## Interpretation limits

- H2 is deliberately used to keep the run disposable and local. Do not treat H2 throughput as MySQL throughput.
- Docker Desktop was unavailable on the measurement host; Compose/MySQL/container metrics were not collected.
- The repository has no Redis, message queue, worker fleet, or ML component, so cache ratio, consumer lag, recovery of external services, and ML quality metrics are not applicable.
- The suite tests only the listed client levels. It does not establish saturation beyond the highest completed level.
