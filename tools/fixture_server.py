#!/usr/bin/env python3
"""Local HTML fixture for Miyomu's M1 source-read demo. No third-party content."""

import argparse
import html
import struct
import zlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

COMICS = [
    ("demo-1", "Miyomu Demo 1", "Miyomu", "#1B2A4A"),
    ("demo-2", "Miyomu Demo 2", "Miyomu", "#E8A33D"),
    ("demo-3", "Miyomu Demo 3", "Miyomu", "#2F6F6B"),
]


def png_bytes(width: int, height: int, rgb: tuple[int, int, int]) -> bytes:
    rows = []
    for y in range(height):
        shade = 0.65 + 0.35 * (y / max(height - 1, 1))
        color = bytes(int(channel * shade) for channel in rgb)
        rows.append(b"\x00" + color * width)
    raw = b"".join(rows)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 6))
        + chunk(b"IEND", b"")
    )


class Handler(BaseHTTPRequestHandler):
    server_version = "MiyomuFixture/1.0"

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        if path == "/health":
            self.respond_text("ok")
        elif path == "/search":
            self.handle_search(query.get("q", [""])[0])
        elif path.startswith("/comic/"):
            self.handle_comic(path.removeprefix("/comic/"))
        elif path.startswith("/chapter/"):
            self.handle_chapter(path.removeprefix("/chapter/"))
        elif path.startswith("/images/"):
            self.handle_image(path.removeprefix("/images/"))
        else:
            self.send_error(404)

    def handle_search(self, keyword: str) -> None:
        links = []
        for comic_id, title, author, _ in COMICS:
            links.append(
                '<a class="comic" data-id="{id}">'
                '<span class="title">{title}</span>'
                '<span class="author">{author}</span>'
                "</a>".format(
                    id=html.escape(comic_id),
                    title=html.escape(title),
                    author=html.escape(author),
                )
            )
        body = (
            "<html><body><h1>Search: {q}</h1><div class=\"results\">{links}</div></body></html>"
        ).format(q=html.escape(keyword), links="".join(links))
        self.respond_html(body, cookie="session=demo; Path=/; HttpOnly; Max-Age=3600")

    def handle_comic(self, comic_id: str) -> None:
        comic = next((item for item in COMICS if item[0] == comic_id), None)
        if comic is None:
            self.send_error(404)
            return
        _, title, author, _ = comic
        chapters = "".join(
            '<a data-id="ep_{id}_{n}">Chapter {n}</a>'.format(id=comic_id, n=n)
            for n in (1, 2, 3)
        )
        body = (
            '<html><body>'
            '<h1 class="title">{title}</h1>'
            '<span class="author">{author}</span>'
            '<div class="chapters">{chapters}</div>'
            "</body></html>"
        ).format(
            title=html.escape(title),
            author=html.escape(author),
            chapters=chapters,
        )
        self.respond_html(body)

    def handle_chapter(self, chapter_id: str) -> None:
        comic_id = chapter_id.removeprefix("ep_").rsplit("_", 1)[0]
        pages = "".join(
            '<img data-src="/images/{comic}-{n}.png">'.format(comic=comic_id, n=n)
            for n in (1, 2, 3)
        )
        body = "<html><body><div class=\"pages\">{pages}</div></body></html>".format(
            pages=pages
        )
        self.respond_html(body)

    def handle_image(self, name: str) -> None:
        stem = name.removesuffix(".png")
        if "-" not in stem:
            self.send_error(404)
            return
        comic_id, page = stem.rsplit("-", 1)
        comic = next((item for item in COMICS if item[0] == comic_id), None)
        if comic is None or not page.isdigit():
            self.send_error(404)
            return
        rgb_hex = comic[3]
        rgb = tuple(int(rgb_hex[i : i + 2], 16) for i in (1, 3, 5))
        data = png_bytes(480, 720, rgb)
        self.send_response(200)
        self.send_header("Content-Type", "image/png")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def respond_html(self, body: str, cookie: str | None = None) -> None:
        data = body.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        if cookie:
            self.send_header("Set-Cookie", cookie)
        self.end_headers()
        self.wfile.write(data)

    def respond_text(self, body: str) -> None:
        data = body.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt: str, *args) -> None:
        print("[fixture] " + fmt % args)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=18080)
    args = parser.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    server.daemon_threads = True
    print(f"Miyomu fixture listening on http://{args.host}:{args.port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
