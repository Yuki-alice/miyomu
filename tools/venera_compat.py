#!/usr/bin/env python3
"""Venera source compatibility audit.

Answers one question: would this third-party Venera comic-source script run
unmodified on Miyomu's JS runtime?

It compares two sides:

  provided  — the host API declared by `SourcePreamble.kt` (the JS injected into
              every source before evaluation), split into *implemented* members
              and *throwing stubs*
  required  — every host API member the source actually references

Nothing is executed and nothing is fetched: this is a static diff, so it is safe
to run against sources you do not control and gives the same answer on CI.

Three verdicts:
  OK       every referenced member is implemented — the read path should run as-is
  RISK     the source references throwing stubs; the read path may still work, but
           the listed calls fail if reached
  BLOCKED  the source references a global we do not define at all

Usage:
  python3 tools/venera_compat.py reference/venera-yuan/manga_dex.js
  python3 tools/venera_compat.py reference/venera-yuan/*.js --summary
  python3 tools/venera_compat.py src.js --json

Exit code: 0 when every audited source is OK, 1 otherwise, 2 on usage errors.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

PREAMBLE = Path("shared/source-runtime/src/commonMain/kotlin/app/miyomu/source/runtime/SourcePreamble.kt")

HOST_GLOBALS = [
    "Network", "HtmlDocument", "HtmlElement", "Comic", "ComicDetails", "Cookie",
    "Convert", "UI", "Std", "APP", "Image", "ImageLoadingConfig", "Comment",
    "setTimeout", "setInterval", "fetch", "console", "log",
    "randomInt", "randomDouble", "createUuid", "getClipboard", "setClipboard", "compute",
]

CONTRACT_MEMBERS = [
    "name", "key", "version", "minAppVersion", "url", "api", "nsfw",
    "search", "comic", "explore", "category", "categoryComics", "categoryComicsPage",
    "settings", "login", "logout", "comments", "loadComments", "loadComicComments",
    "onImageLoad", "onThumbnailLoad", "onClickTag", "initialize", "loadData", "saveData",
]

STUB_MARKERS = ("is not supported", "is not available", "not implemented")


def preamble_js() -> str:
    text = PREAMBLE.read_text()
    start = text.index('"""') + 3
    return text[start: text.index('"""', start)]


def _block_end(text: str, open_at: int) -> int:
    depth = 0
    for i in range(open_at, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    return -1


def provided_api(js: str) -> dict[str, dict[str, str]]:
    """global name -> {member: "impl" | "stub"}."""
    api: dict[str, dict[str, str]] = {}

    pattern = re.compile(
        r"globalThis\.(\w+)\s*=\s*(class\s+)?(?:[\w$]+\s*)?(?:extends\s+[\w.$]+\s*)?\{"
    )
    for match in pattern.finditer(js):
        name = match.group(1)
        open_at = match.end() - 1
        close_at = _block_end(js, open_at)
        if close_at < 0:
            continue
        body = js[open_at + 1: close_at]
        members: dict[str, str] = {}
        depth = 0
        cursor = 0
        for line in body.splitlines():
            stripped = line.strip()
            if depth == 0:
                m = re.match(r"(?:async\s+|static\s+|get\s+|set\s+)*([\w$]+)\s*(?:\(|=|:)", stripped)
                if m and m.group(1) not in {"if", "for", "while", "return", "const", "let", "var"}:
                    state = "impl"
                    brace = body.find("{", cursor)
                    if brace >= 0:
                        end = _block_end(body, brace)
                        if end > 0 and any(marker in body[brace:end] for marker in STUB_MARKERS):
                            state = "stub"
                    members[m.group(1)] = state
            depth += line.count("{") - line.count("}")
            cursor += len(line) + 1
        api[name] = members

    for match in re.finditer(r"globalThis\.(\w+)\s*=\s*(?!class|\{)", js):
        api.setdefault(match.group(1), {})
    return api


def strip_comments(js: str) -> str:
    js = re.sub(r"/\*.*?\*/", "", js, flags=re.S)
    return re.sub(r"(?m)^\s*//.*$", "", js)


def required_api(js: str) -> tuple[dict[str, set[str]], set[str]]:
    body = strip_comments(js)
    used: dict[str, set[str]] = {}
    for name in HOST_GLOBALS:
        members = set(re.findall(rf"\b{name}\s*\.\s*([\w$]+)", body))
        members |= set(re.findall(rf"\b{name}\s*\[\s*['\"]([\w$]+)['\"]\s*\]", body))
        constructed = re.search(rf"\bnew\s+{name}\s*\(", body) is not None
        if members or constructed:
            used[name] = members

    contract = {m for m in CONTRACT_MEMBERS if re.search(rf"(?m)^\s*{m}\s*[:=]", body)}
    if re.search(r"\bthis\.(loadData|saveData|deleteData|loadSetting)\b", body):
        contract.add("this.*Data")
    return used, contract


def audit(source: Path, api: dict[str, dict[str, str]]) -> dict:
    js = source.read_text()
    used, contract = required_api(js)

    implemented: dict[str, list[str]] = {}
    stubs: dict[str, list[str]] = {}
    absent: dict[str, list[str]] = {}

    for name, members in sorted(used.items()):
        have = api.get(name)
        if have is None:
            absent[name] = sorted(members) or ["<constructor>"]
            continue
        for member in sorted(members):
            state = have.get(member)
            if state == "impl":
                implemented.setdefault(name, []).append(member)
            elif state == "stub":
                stubs.setdefault(name, []).append(member)
            else:
                absent.setdefault(name, []).append(member)

    verdict = "BLOCKED" if absent else ("RISK" if stubs else "OK")
    return {
        "source": str(source),
        "is_comic_source": bool(re.search(r"class\s+\w+\s+extends\s+ComicSource", js)),
        "verdict": verdict,
        "contract": sorted(contract),
        "implemented": implemented,
        "stubs": stubs,
        "absent": absent,
        "ok": verdict == "OK",
    }


def render(result: dict) -> str:
    lines = [f"\n=== {Path(result['source']).name} ==="]
    if not result["is_comic_source"]:
        lines.append("  (no `class X extends ComicSource` — not a comic source, skipped)")
        return "\n".join(lines)
    lines.append(f"  contract  {', '.join(result['contract']) or '(none)'}")
    for name, members in result["implemented"].items():
        joined = ", ".join(f"{name}.{m}" for m in members)
        lines.append(f"  ok        {joined}" if len(members) <= 4 else f"  ok        {name} ({len(members)} members)")
    for name, members in result["stubs"].items():
        lines.append("  STUB      " + ", ".join(f"{name}.{m}" for m in members) + "   <- throwing stub")
    for name, members in result["absent"].items():
        lines.append("  ABSENT    " + ", ".join(f"{name}.{m}" for m in members) + "   <- not defined")
    lines.append(f"  verdict   {result['verdict']}")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit Venera sources against Miyomu's host API.")
    parser.add_argument("sources", nargs="+", type=Path)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--summary", action="store_true")
    args = parser.parse_args()

    if not PREAMBLE.is_file():
        print(f"run from the repository root (missing {PREAMBLE})", file=sys.stderr)
        return 2

    api = provided_api(preamble_js())
    results = [r for r in (audit(path, api) for path in args.sources) if r["is_comic_source"]]

    if args.json:
        print(json.dumps({"provided": api, "results": results}, indent=2, ensure_ascii=False))
    elif args.summary:
        for r in results:
            gaps = sum(len(v) for v in r["stubs"].values()) + sum(len(v) for v in r["absent"].values())
            print(f"{r['verdict']:<8}{Path(r['source']).name:<20} gaps={gaps}")
    else:
        for r in results:
            print(render(r))

    return 0 if all(r["ok"] for r in results) else 1


if __name__ == "__main__":
    sys.exit(main())
