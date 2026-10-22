# 商品查询接口轻量基线：用户手动运行

2026-10-22结果更新：用户已运行正式脚本并贴结果，30成功/0失败，mean9.89/P509.14/P9514.94/max17.49ms、实际成功rps101.08、双上游各15。已归档PRODUCT_BASELINE_REPORT.md与verification/product-baseline-2026-10-22.json。下面“尚未运行”是本课准备时记录；无需重复执行来凑指标。

2026-10-22：助手只读确认恢复实验库Exited，runtime六healthy；真实GET /api/products?page=1&size=10返回200和4条在售（数据库共6行）。Linux Python3.12.3、3.8Gi总内存/约2.0Gi可用，之前VM配置2vCPU，镜像均在本地。尚未运行测量。源码入口为ProductController.list→ProductService.findOnSalePage→数据库分页查询→DTO→JSON。无模型调用、无登录、无业务写入。

## 用户新建D:\ShopAgent-X\deploy\scripts\measure_products.py

```python
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
```

仅用标准库，在Linux宿主运行，不使用Windowsvenv或容器Python。助手只对候选AST语法解析，未执行预热或测量；未创建正式脚本/上传。计时为单次HTTP打开连接+读取解析JSON直到关闭响应，不复用连接池；每请求5秒超时，预热失败直接停止，不在服务未就绪时继续测量。成功判定为200且data为对象/items为列表，失败纳入总次数/成功率，延迟统计仅成功请求，吞吐为本次成功数/完整测量墙钟时间。

## 用户Windows上传脚本

```powershell
scp -F "D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config" "D:\ShopAgent-X\deploy\scripts\measure_products.py" shopagent-linux:shopagent/deploy/scripts/
```

## 用户Linux执行

```bash
cd /home/shopagent/shopagent/deploy
python3 scripts/measure_products.py
```

一次即可。结果与错误可反馈，不含凭证或业务正文。无需启动IDEA/PyCharm，也不安装压测工具。保留网站运行，实验Cluster/恢复库继续停止。

## 结果怎么讲

- 5次预热排除首轮初始化影响，但不能保证所有缓存完全稳定；预热不纳入30次测量。
- 并发1/30次串行，不是并发负载或极限容量测试。
- mean平均、p50中位、p95最近秩ceil(0.95*n)-1、max最大值，仅成功请求。
- observed_successful_rps是此次顺序请求实际达到的速率，不是系统最大QPS。
- upstream_counts读取Nginx调试响应头，观察本次成功响应的路由，地址为当次容器IP；多地址头可能包含失败重试，不能据此断言每个地址都成功。
- LinuxVM内127.0.0.1→Nginx→双Java→MySQL，小数据6商品/4在售，响应通常更快；不含WindowsSSH、浏览器资源、真实互联网延迟或DeepSeek耗时。
- 测量前后服务检查与样本结果可独立核对，不能把本基线包装成高并发提升或生产容量。没有停一个实例做同条件A/B对照，不报告双实例提速百分比。

用户结果后助手分析并写入实测报告，再README/演示/贡献台账。当前无测量数字，不能预填漂亮指标。

