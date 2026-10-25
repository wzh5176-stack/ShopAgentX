"""使用本机 MySQL 二进制创建临时实例，完成回归后正常关闭；不操作已安装实例。"""
import argparse
from pathlib import Path
import os
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql-bin", required=True, help="directory containing mysqld, mysql and mysqladmin")
    parser.add_argument("--maven", required=True)
    parser.add_argument("--java-home", required=True)
    args = parser.parse_args()
    binaries = Path(args.mysql_bin).resolve()
    suffix = ".exe" if os.name == "nt" else ""
    tools = {name: binaries / (name + suffix) for name in ("mysqld", "mysql", "mysqladmin")}
    if any(not path.is_file() for path in tools.values()):
        parser.error("all three MySQL executables must exist")
    output = ROOT / "tmp/regression-mysql"
    output.mkdir(parents=True, exist_ok=True)
    # 新分配目录有独立来源；不接受用户传入现有数据目录，不删除已有实例目录。
    temporary = Path(tempfile.mkdtemp(prefix="shopagent-mysql-regression-")).resolve()
    process = None
    try:
        data = temporary / "data"
        data.mkdir()
        initialized = subprocess.run([str(tools["mysqld"]), "--no-defaults", "--initialize-insecure",
                                      f"--basedir={binaries.parent}", f"--datadir={data}"],
                                     capture_output=True, timeout=90)
        if initialized.returncode:
            (output / "initialize.log").write_bytes(initialized.stdout + initialized.stderr)
            raise RuntimeError("temporary MySQL initialization failed; see tmp/regression-mysql/initialize.log")
        with socket.socket() as available:
            available.bind(("127.0.0.1", 0))
            port = available.getsockname()[1]
        env = os.environ.copy()
        env["PYTHONUTF8"] = "1"
        password = secrets.token_hex(24)
        env["MYSQL_PWD"] = password
        # 禁止客户端隐式消费本机 mysql_config_editor 的登录配置。
        env["MYSQL_TEST_LOGIN_FILE"] = str(temporary / "no-login-file")
        connection = ["--no-defaults", "--protocol=TCP", "--host=127.0.0.1", f"--port={port}", "--user=shopagent_test", "--connect-timeout=2"]
        bootstrap = temporary / "bootstrap.sql"
        bootstrap.write_text(
            "CREATE DATABASE shopagent_regression CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
            f"CREATE USER 'shopagent_test'@'127.0.0.1' IDENTIFIED BY '{password}';\n"
            "GRANT ALL ON shopagent_regression.* TO 'shopagent_test'@'127.0.0.1';\n"
            "GRANT SHUTDOWN ON *.* TO 'shopagent_test'@'127.0.0.1';\n"
            f"ALTER USER 'root'@'localhost' IDENTIFIED BY '{secrets.token_hex(24)}';\n", encoding="utf-8")
        with (output / "server.log").open("wb") as log:
            try:
                process = subprocess.Popen([str(tools["mysqld"]), "--no-defaults", f"--basedir={binaries.parent}",
                                            f"--datadir={data}", "--bind-address=127.0.0.1", f"--port={port}",
                                            "--mysqlx=OFF", "--skip-name-resolve", "--innodb-buffer-pool-size=64M",
                                            f"--init-file={bootstrap.as_posix()}"],
                                           stdout=log, stderr=subprocess.STDOUT, env=env)
                for attempt in range(60):
                    if process.poll() is not None:
                        raise RuntimeError("temporary MySQL exited; see server.log")
                    probe = subprocess.run([str(tools["mysql"]), *connection, "--execute=SELECT 1"], env=env, capture_output=True)
                    if probe.returncode == 0:
                        break
                    time.sleep(0.5)
                else:
                    raise RuntimeError("temporary MySQL did not become ready")
                bootstrap.unlink()
                env.update({"SHOPAGENT_TEST_MYSQL_ALLOW": "empty-regression-db",
                            "SHOPAGENT_TEST_MYSQL_URL": f"jdbc:mysql://127.0.0.1:{port}/shopagent_regression?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                            "SHOPAGENT_TEST_MYSQL_USER": "shopagent_test", "SHOPAGENT_TEST_MYSQL_PASSWORD": password})
                print(f"Isolated MySQL ready on loopback port {port}; running full regression.", flush=True)
                return subprocess.call([sys.executable, str(ROOT / "scripts/regression.py"), "--maven", args.maven,
                                        "--java-home", args.java_home, "--mysql"], cwd=ROOT, env=env)
            finally:
                if process is not None and process.poll() is None:
                    stopped = subprocess.run([str(tools["mysqladmin"]), *connection, "shutdown"], env=env, capture_output=True, timeout=30)
                    if stopped.returncode:
                        # 不强杀数据库；留下数据目录供诊断，且令本次回归失败。
                        raise RuntimeError("temporary MySQL graceful shutdown failed")
                    process.wait(timeout=30)
                    print("Temporary MySQL stopped normally.", flush=True)
    finally:
        # 只有自己创建的临时实例停止后才清理目录；失败不碰仍在运行的数据库文件。
        if process is None or process.poll() is not None:
            if temporary.parent == Path(tempfile.gettempdir()).resolve() and temporary.name.startswith("shopagent-mysql-regression-"):
                shutil.rmtree(temporary)
        else:
            print(f"Temporary database retained for diagnosis: {temporary}", file=sys.stderr)


if __name__ == "__main__":
    raise SystemExit(main())
