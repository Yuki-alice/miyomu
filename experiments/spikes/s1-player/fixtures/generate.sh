#!/usr/bin/env bash
# Generates local test media. Output files are gitignored; subtitles are tracked.
set -euo pipefail
cd "$(dirname "$0")"

ffmpeg -y -f lavfi -i testsrc2=size=1280x720:rate=30 -f lavfi -i sine=frequency=440 \
  -t 60 -c:v libx264 -preset veryfast -pix_fmt yuv420p -c:a aac -b:a 128k base.mp4

ffmpeg -y -i base.mp4 -c copy base.mkv

mkdir -p hls
ffmpeg -y -i base.mp4 -c copy -f hls -hls_time 6 -hls_playlist_type vod hls/index.m3u8

ffmpeg -y -f lavfi -i testsrc2=size=1280x720:rate=30 -f lavfi -i sine=frequency=440 \
  -t 12 -c:v libx265 -preset ultrafast -tag:v hvc1 -c:a aac -b:a 128k hevc.mp4

echo "Fixtures ready: base.mp4, base.mkv, hls/index.m3u8, hevc.mp4"
