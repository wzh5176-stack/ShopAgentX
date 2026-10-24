from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from http.client import HTTPConnection
import argparse

NOTIFY_PATH = "/api/payments/alipay/notify"
UPSTREAM_PORT = 8888


class NotifyRelay(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(15)

    def log_message(self, format, *args):
        # 不记录通知正文，避免打印交易信息。
        pass

    def reply(self, status, body):
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        self.reply(404, b"not found")

    def do_POST(self):
        if self.path != NOTIFY_PATH:
            self.reply(404, b"not found")
            return

        lengths = self.headers.get_all("Content-Length", [])
        if len(lengths) != 1 or self.headers.get("Transfer-Encoding"):
            self.reply(400, b"failure")
            return

        try:
            length = int(lengths[0])
        except ValueError:
            self.reply(400, b"failure")
            return

        if not 0 < length <= 65536:
            self.reply(413, b"failure")
            return

        upstream = HTTPConnection("127.0.0.1", UPSTREAM_PORT, timeout=15)
        try:
            body = self.rfile.read(length)
            if len(body) != length:
                self.reply(400, b"failure")
                return

            upstream.request(
                "POST",
                NOTIFY_PATH,
                body=body,
                headers={
                    "Content-Type": self.headers.get("Content-Type", ""),
                    "Content-Length": str(length),
                },
            )
            response = upstream.getresponse()
            result = response.read(1024)
            self.reply(response.status, result)
            print(f"Java notification response: HTTP {response.status}")
        except (OSError, TimeoutError):
            self.reply(502, b"failure")
        finally:
            upstream.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream-port", type=int, default=8888)
    args = parser.parse_args()
    if not 1 <= args.upstream_port <= 65535:
        parser.error("upstream port must be between 1 and 65535")
    UPSTREAM_PORT = args.upstream_port
    server = ThreadingHTTPServer(("127.0.0.1", 8899), NotifyRelay)
    print(f"Notification relay listening on 127.0.0.1:8899 -> 127.0.0.1:{UPSTREAM_PORT}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
