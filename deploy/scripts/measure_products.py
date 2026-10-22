import json
import math
import statistics
import time
from collections import Counter
from urllib.request import urlopen

URL = "http://127.0.0.1:8080/api/products?page=1&size=10"
WARMUP = 5
SAMPLES = 30


def fetch_products():
    started = time.perf_counter()
    with urlopen(URL, timeout=5) as response:
        body = json.load(response)
        data = body.get("data")
        if response.status != 200 or not isinstance(data, dict):
            raise ValueError("Unexpected products response")
        if not isinstance(data.get("items"), list):
            raise ValueError("Missing product items")
        upstream = response.headers.get("X-ShopAgent-Upstream", "unknown")
    elapsed_ms = (time.perf_counter() - started) * 1000
    return elapsed_ms, upstream


for _ in range(WARMUP):
    fetch_products()

latencies = []
upstreams = Counter()
errors = []
started = time.perf_counter()

for index in range(SAMPLES):
    try:
        elapsed_ms, upstream = fetch_products()
        latencies.append(elapsed_ms)
        upstreams[upstream] += 1
    except Exception as error:
        errors.append({"request": index + 1, "error": str(error)})

elapsed = time.perf_counter() - started
ordered = sorted(latencies)
result = {
    "url": URL,
    "concurrency": 1,
    "warmup_requests": WARMUP,
    "measured_requests": SAMPLES,
    "successes": len(latencies),
    "failures": len(errors),
    "success_rate_percent": round(len(latencies) * 100 / SAMPLES, 2),
    "mean_ms": round(statistics.mean(latencies), 2) if ordered else None,
    "p50_ms": round(statistics.median(latencies), 2) if ordered else None,
    "p95_ms": round(ordered[math.ceil(len(ordered) * 0.95) - 1], 2) if ordered else None,
    "max_ms": round(ordered[-1], 2) if ordered else None,
    "elapsed_seconds": round(elapsed, 3),
    "observed_successful_rps": round(len(latencies) / elapsed, 2),
    "upstream_counts": dict(upstreams),
    "errors": errors,
}
print(json.dumps(result, ensure_ascii=False, indent=2))
raise SystemExit(1 if errors else 0)