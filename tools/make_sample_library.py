#!/usr/bin/env python3
"""Generate a self-contained sample local library for Miyomu.

All images are generated from solid colors; no third-party content is used.
"""

import argparse
import struct
import zlib
import zipfile
from pathlib import Path


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


COLORS = [
    (27, 42, 74),
    (232, 163, 61),
    (47, 111, 107),
    (120, 80, 160),
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--target",
        type=Path,
        default=Path.home() / ".miyomu" / "library",
        help="Library root (default: ~/.miyomu/library)",
    )
    args = parser.parse_args()
    root: Path = args.target
    root.mkdir(parents=True, exist_ok=True)

    directory_book = root / "Sample Directory Book"
    for chapter in range(1, 4):
        chapter_dir = directory_book / f"chapter {chapter}"
        chapter_dir.mkdir(parents=True, exist_ok=True)
        for page in range(1, 4):
            color = COLORS[(chapter + page) % len(COLORS)]
            (chapter_dir / f"{page}.png").write_bytes(png_bytes(320, 480, color))

    zip_path = root / "Sample Zip Book.cbz"
    with zipfile.ZipFile(zip_path, "w") as archive:
        for page, color in enumerate(COLORS, start=1):
            archive.writestr(f"{page}.png", png_bytes(320, 480, color))

    print(f"Sample library: {root}")
    print(f"  {directory_book.name}: 3 chapters x 3 pages")
    print(f"  {zip_path.name}: {len(COLORS)} pages")


if __name__ == "__main__":
    main()
