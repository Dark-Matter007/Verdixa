#!/usr/bin/env python3
"""Dependency-free concurrent HTTP benchmark for Verdixa's local benchmark profile."""
from __future__ import annotations

import argparse
import concurrent.futures
import datetime as dt
import json
import platform
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path


def request(url: str, headers: dict[str, str] | None = None, data: dict | None = None) -> tuple[int, float, str]:
    payload = json.dumps(data).encode() if data is not None else None
    request_headers = {"Content-Type": "application/json"} if payload else {}
    request_headers.update(headers or {})
    started = time.perf_counter_ns()
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=payload, headers=request_headers), timeout=30) as response:
            return response.status, (time.perf_counter_ns() - started) / 1_000_000, response.read().decode()
    except urllib.error.HTTPError as error:
        return error.code, (time.perf_counter_ns() - started) / 1_000_000, error.read().decode()
    except Exception as error:  # recorded as an error; never concealed
        return 0, (time.perf_counter_ns() - started) / 1_000_000, str(error)


def percentile(values: list[float], fraction: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lower, upper = int(position), min(int(position) + 1, len(ordered) - 1)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--endpoint", default="/api/problems/library?size=100")
    parser.add_argument("--concurrency", type=int, required=True)
    parser.add_argument("--duration-seconds", type=int, required=True)
    parser.add_argument("--warmup-seconds", type=int, default=10)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.concurrency < 1 or args.duration_seconds < 1 or args.warmup_seconds < 0:
        parser.error("concurrency and duration must be positive; warmup cannot be negative")

    login_status, _, login_body = request(args.base_url + "/api/auth/login", data={
        "username": "benchmark-user", "password": "benchmark-password"})
    if login_status != 200:
        raise RuntimeError(f"benchmark login failed with HTTP {login_status}: {login_body}")
    token = json.loads(login_body)["token"]
    headers = {"Authorization": f"Bearer {token}"}
    url = args.base_url + args.endpoint

    def one() -> tuple[int, float]:
        status, elapsed, _ = request(url, headers=headers)
        return status, elapsed

    # Warm-up uses the same endpoint and concurrency but is excluded from results.
    warmup_end = time.monotonic() + args.warmup_seconds
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        while time.monotonic() < warmup_end:
            list(pool.map(lambda _: one(), range(args.concurrency)))

        stop_at = time.monotonic() + args.duration_seconds
        latencies: list[float] = []
        status_counts: dict[str, int] = {}
        lock = threading.Lock()

        def worker() -> None:
            while time.monotonic() < stop_at:
                status, elapsed = one()
                with lock:
                    latencies.append(elapsed)
                    status_counts[str(status)] = status_counts.get(str(status), 0) + 1

        started = time.monotonic()
        futures = [pool.submit(worker) for _ in range(args.concurrency)]
        for future in futures:
            future.result()
        elapsed_seconds = time.monotonic() - started

    total = len(latencies)
    successful = status_counts.get("200", 0)
    result = {
        "schema_version": 1,
        "timestamp_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "tool": "benchmarks/http_benchmark.py (Python standard library)",
        "host": {"os": platform.platform(), "python": sys.version.split()[0]},
        "workload": {"endpoint": args.endpoint, "concurrency": args.concurrency,
                     "warmup_seconds": args.warmup_seconds, "measurement_seconds": args.duration_seconds,
                     "seed": {"users": 1, "problems": 100, "submissions": 1000}},
        "results": {"requests": total, "successful_requests": successful,
                    "status_counts": status_counts, "elapsed_seconds": round(elapsed_seconds, 6),
                    "throughput_rps": round(total / elapsed_seconds, 3) if elapsed_seconds else 0,
                    "error_rate_percent": round((total - successful) * 100 / total, 4) if total else 100,
                    "latency_ms": {"min": round(min(latencies), 3) if latencies else 0,
                                   "mean": round(statistics.fmean(latencies), 3) if latencies else 0,
                                   "p50": round(percentile(latencies, .50), 3),
                                   "p95": round(percentile(latencies, .95), 3),
                                   "p99": round(percentile(latencies, .99), 3),
                                   "max": round(max(latencies), 3) if latencies else 0}},
        "raw_latency_ms": [round(value, 3) for value in latencies],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result["results"], indent=2))
    return 0 if successful == total else 2


if __name__ == "__main__":
    raise SystemExit(main())
