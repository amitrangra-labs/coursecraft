#!/usr/bin/env python3
"""Upload a lecture video to the Internet Archive and print its public URL.

The Internet Archive is free, no bandwidth limits, and its download URLs support
HTTP range requests — so ExoPlayer streams them inline. Everything uploaded here is
PUBLIC; only use it for openly-licensed / public educational content.

Setup:
  1. Create a free account at https://archive.org
  2. Get your S3-like keys at https://archive.org/account/s3.php
  3. export IA_ACCESS_KEY=... IA_SECRET_KEY=...   (these are secrets — keep them safe)

Usage:
  python3 tools/ia_upload.py <file.mp4> "<Lecture title>" [identifier]

Then paste the printed "Public URL" into the app's Add-lecture field — CourseCraft
detects the http(s) URL and plays it natively via ExoPlayer.
"""
import os
import re
import sys
import time
import urllib.error
import urllib.request


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)

    path, title = sys.argv[1], sys.argv[2]
    access = os.environ.get("IA_ACCESS_KEY")
    secret = os.environ.get("IA_SECRET_KEY")
    if not access or not secret:
        print("Missing IA_ACCESS_KEY / IA_SECRET_KEY — get them at https://archive.org/account/s3.php")
        sys.exit(1)
    if not os.path.isfile(path):
        print(f"No such file: {path}")
        sys.exit(1)

    filename = os.path.basename(path)
    if len(sys.argv) > 3:
        identifier = sys.argv[3]
    else:
        slug = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")[:40] or "lecture"
        identifier = f"coursecraft-{slug}-{int(time.time())}"

    ctype = "video/mp4"
    if filename.lower().endswith((".m3u8",)):
        ctype = "application/vnd.apple.mpegurl"
    elif filename.lower().endswith((".webm",)):
        ctype = "video/webm"

    url = f"https://s3.us.archive.org/{identifier}/{filename}"
    with open(path, "rb") as f:
        data = f.read()

    req = urllib.request.Request(
        url,
        data=data,
        method="PUT",
        headers={
            "authorization": f"LOW {access}:{secret}",
            "x-amz-auto-make-bucket": "1",           # create the item if it doesn't exist
            "x-archive-meta-mediatype": "movies",
            "x-archive-meta-title": title,
            # Optional: put it in a collection you can write to, e.g. opensource_movies.
            # "x-archive-meta-collection": "opensource_movies",
            "Content-Type": ctype,
        },
    )
    print(f"Uploading {filename} ({len(data)} bytes) to item '{identifier}' ...")
    try:
        with urllib.request.urlopen(req, timeout=1800) as r:
            print("Upload HTTP", r.status)
    except urllib.error.HTTPError as e:
        print("ERROR", e.code, e.read().decode()[:500])
        sys.exit(1)

    download = f"https://archive.org/download/{identifier}/{filename}"
    print()
    print("Identifier    :", identifier)
    print("Public URL    :", download, "  <-- use this as the lecture video")
    print("Details page  :", f"https://archive.org/details/{identifier}")
    print("\nNote: the item may take ~30-60s to become fully available.")


if __name__ == "__main__":
    main()
