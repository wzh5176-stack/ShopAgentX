"""默认运行离线回归；--mysql 显式加入专用空库集成测试。不启动业务服务。"""
import argparse
from datetime import datetime, timezone
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven", default=shutil.which("mvn") or shutil.which("mvn.cmd"))
    parser.add_argument("--java-home", default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--python", dest="python_path")
    parser.add_argument("--mysql", action="store_true")
    args = parser.parse_args()
    if not args.maven:
        parser.error("Maven not found; use --maven with the full executable path")
    venv_python = ROOT / "ai-service/.venv" / ("Scripts/python.exe" if os.name == "nt" else "bin/python")
    python = args.python_path or (str(venv_python) if venv_python.exists() else sys.executable)
    env = os.environ.copy()
    env["PYTHONUTF8"] = "1"
    env["MAVEN_OPTS"] = env.get("MAVEN_OPTS", "") + " -Dfile.encoding=UTF-8"
    env.pop("DEEPSEEK_API_KEY", None)
    if args.java_home:
        env["JAVA_HOME"] = args.java_home
        env["PATH"] = str(Path(args.java_home) / "bin") + os.pathsep + env.get("PATH", "")
    if args.mysql and env.get("SHOPAGENT_TEST_MYSQL_ALLOW") != "empty-regression-db":
        parser.error("mysql tests require explicit empty-regression-db opt-in; see docs/REGRESSION.md")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    output = ROOT / "tmp/regression" / stamp
    output.mkdir(parents=True)
    jobs = [
        ("sql-catalog", [python, "scripts/check_migrations.py"], ROOT),
        ("engineering", [python, "-m", "unittest", "discover", "-s", "scripts/tests", "-v"], ROOT),
        ("python", [python, "-m", "unittest", "discover", "-s", "tests", "-v"], ROOT / "ai-service"),
        ("java", [args.maven, "-B", "-ntp"] + (["-Pmysql-it"] if args.mysql else []) + ["clean", "verify"], ROOT / "backend"),
    ]
    if args.mysql:
        jobs.append(('catalog-mysql', [python, 'scripts/catalog_mysql_it.py'], ROOT))
    results = []
    for name, command, directory in jobs:
        print(f"\n[{name}] running", flush=True)
        with (output / f"{name}.log").open("w", encoding="utf-8") as log:
            try:
                process = subprocess.Popen(command, cwd=directory, env=env, stdout=subprocess.PIPE,
                                           stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
                for line in process.stdout:
                    log.write(line)
                    print(line, end="", flush=True)
                code = process.wait()
            except OSError as error:
                log.write(str(error))
                print(f"{name}: executable could not start")
                code = 1
        result = {"name": name, "exit_code": code}
        if name in {"engineering", "python", "catalog-mysql"}:
            matches = re.findall(r"Ran (\d+) tests?", (output / f"{name}.log").read_text(encoding="utf-8"))
            result["tests"] = int(matches[-1]) if matches else 0
            if result["tests"] == 0:
                result["exit_code"] = 1
        results.append(result)
    suites = []
    for folder in ("surefire-reports", "failsafe-reports"):
        for report in sorted((ROOT / "backend/target" / folder).glob("TEST-*.xml")):
            suite = ET.parse(report).getroot()
            suites.append({"suite": suite.get("name"), **{key: int(suite.get(key, 0)) for key in ("tests", "errors", "failures", "skipped")}})
    java_ok = bool(suites) and all(suite["errors"] == suite["failures"] == suite["skipped"] == 0 for suite in suites)
    if args.mysql:
        java_ok = java_ok and any(suite["suite"].endswith("MySqlTradeIT") and suite["tests"] > 0 for suite in suites)
    result = {"passed": all(job["exit_code"] == 0 for job in results) and java_ok, "mysql_requested": args.mysql,
              "jobs": results, "java_suites": suites,
              "scope": "isolated regression; not live payment, model-quality or deployment acceptance"}
    (output / "summary.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nReport: {output / 'summary.json'}")
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
