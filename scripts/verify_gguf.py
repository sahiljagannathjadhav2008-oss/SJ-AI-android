#!/usr/bin/env python3
"""Fast integrity checks for the SJ AI Phase-1 GGUF artifact."""
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1])
if not p.is_file(): raise SystemExit(f"FAIL: missing model: {p}")
size = p.stat().st_size
if size < 100_000_000: raise SystemExit(f"FAIL: model unexpectedly small: {size} bytes")
with p.open("rb") as f: magic = f.read(4)
if magic != b"GGUF": raise SystemExit(f"FAIL: invalid GGUF magic: {magic!r}")
h = hashlib.sha256()
with p.open("rb") as f:
    for chunk in iter(lambda: f.read(8 * 1024 * 1024), b""): h.update(chunk)
print(f"PASS: GGUF magic; size={size:,} bytes; sha256={h.hexdigest()}")
