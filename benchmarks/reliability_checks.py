#!/usr/bin/env python3
"""Non-destructive HTTP reliability checks for a running local benchmark server."""
from __future__ import annotations

import argparse
import datetime as dt
import json
import urllib.error
import urllib.request
from pathlib import Path


def raw_request(url: str, method: str = "GET", body: bytes | None = None, headers: dict[str, str] | None = None) -> tuple[int, str]:
    try:
        request = urllib.request.Request(url, data=body, headers=headers or {}, method=method)
        with urllib.request.urlopen(request, timeout=10) as response:
            return response.status, response.read().decode()
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()
    except Exception as error:
        return 0, str(error)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    base = args.base_url.rstrip("/")
    cases = []
    for name, url, method, body, headers in [
        ("unauthenticated-protected-library", base + "/api/problems/library", "GET", None, {}),
        ("malformed-login-json", base + "/api/auth/login", "POST", b'{"username":', {"Content-Type": "application/json"}),
        ("invalid-login-credentials", base + "/api/auth/login", "POST",
         b'{"username":"benchmark-user","password":"incorrect"}', {"Content-Type": "application/json"}),
    ]:
        status, body_text = raw_request(url, method, body, headers)
        cases.append({"name": name, "http_status": status, "response_bytes": len(body_text.encode()), "response_excerpt": body_text[:200]})
    result = {"schema_version": 1, "timestamp_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
              "scope": "HTTP request validation and authentication failure handling; no external infrastructure modified.",
              "cases": cases}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
