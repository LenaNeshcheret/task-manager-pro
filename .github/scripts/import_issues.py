#!/usr/bin/env python3
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path


REPO = os.environ.get("REPO", "")
ISSUE_FILE = os.environ.get("ISSUE_FILE", "issue-import/pending/issues.md")
ISSUE_LIST_LIMIT = int(os.environ.get("ISSUE_LIST_LIMIT", "1000"))
AUTO_CREATE_LABELS = os.environ.get("AUTO_CREATE_LABELS", "true").lower() in ("1", "true", "yes", "on")


def sh(cmd: list[str], check: bool = True) -> subprocess.CompletedProcess:
    print("+", " ".join(cmd))
    return subprocess.run(cmd, text=True, capture_output=True, check=check)


def normalize_title(title: str) -> str:
    t = re.sub(r"\s+", " ", title.strip())
    return t.lower()


def make_importer_id_from_title(title: str) -> str:
    return hashlib.sha256(normalize_title(title).encode("utf-8")).hexdigest()[:12]


def parse_issue_blocks(text: str):
    blocks = [b.strip() for b in re.split(r"\n---\n", text.strip()) if b.strip()]
    parsed = []

    for idx, block in enumerate(blocks, start=1):
        m = re.search(r"^##\s*Issue:\s*(.+)$", block, re.MULTILINE)
        if not m:
            print(f"[Block {idx}] Skipped: missing '## Issue: ...'")
            continue

        title = m.group(1).strip()

        labels_match = re.search(r"^Labels:\s*(.+)$", block, re.MULTILINE | re.IGNORECASE)
        assignees_match = re.search(r"^Assignees:\s*(.+)$", block, re.MULTILINE | re.IGNORECASE)

        labels = [x.strip() for x in labels_match.group(1).split(",")] if labels_match else []
        labels = [x for x in labels if x]

        assignees = [x.strip() for x in assignees_match.group(1).split(",")] if assignees_match else []
        assignees = [x for x in assignees if x]

        body = re.sub(r"^##\s*Issue:\s*.+$\n?", "", block, flags=re.MULTILINE)
        body = re.sub(r"^Labels:\s*.+$\n?", "", body, flags=re.MULTILINE | re.IGNORECASE)
        body = re.sub(r"^Assignees:\s*.+$\n?", "", body, flags=re.MULTILINE | re.IGNORECASE)
        body = body.strip() or "_No description provided._"

        parsed.append({
            "title": title,
            "labels": labels,
            "assignees": assignees,
            "body": body,
            "importer_id": make_importer_id_from_title(title),
            "block_index": idx,
        })

    return parsed


def get_existing_importer_ids(repo: str) -> set[str]:
    proc = sh([
        "gh", "issue", "list",
        "--repo", repo,
        "--state", "all",
        "--limit", str(ISSUE_LIST_LIMIT),
        "--json", "number,title,body",
    ], check=False)

    if proc.returncode != 0:
        print("Failed to list issues.")
        print(proc.stdout)
        print(proc.stderr)
        sys.exit(proc.returncode)

    issues = json.loads(proc.stdout or "[]")
    found = set()

    for issue in issues:
        body = issue.get("body") or ""
        for match in re.finditer(r"<!--\s*importer-id:\s*([a-f0-9]{6,64})\s*-->", body, re.IGNORECASE):
            found.add(match.group(1).lower())

    print(f"Found {len(found)} existing importer IDs in current issue list scan.")
    return found


def get_existing_labels(repo: str) -> set[str]:
    proc = sh([
        "gh", "label", "list",
        "--repo", repo,
        "--limit", "1000",
        "--json", "name",
    ], check=False)

    if proc.returncode != 0:
        print("Warning: failed to list labels; proceeding without label pre-check.")
        print(proc.stdout)
        print(proc.stderr)
        return set()

    try:
        data = json.loads(proc.stdout or "[]")
        return {item["name"] for item in data if "name" in item}
    except Exception:
        return set()


def ensure_labels_exist(repo: str, labels: list[str], known_labels: set[str]) -> list[str]:
    if not labels:
        return []

    valid = []

    for label in labels:
        if label in known_labels:
            valid.append(label)
            continue

        if not AUTO_CREATE_LABELS:
            print(f"Label not found, skipping label: {label}")
            continue

        print(f"Creating missing label: {label}")
        proc = sh(["gh", "label", "create", label, "--repo", repo], check=False)

        # If create failed because label already exists (race condition), still allow it
        if proc.returncode != 0:
            stderr = (proc.stderr or "").lower()
            stdout = (proc.stdout or "").lower()
            if "already exists" in stderr or "already exists" in stdout:
                pass
            else:
                print(f"Warning: failed to create label '{label}', skipping label.")
                print(proc.stdout)
                print(proc.stderr)
                continue

        known_labels.add(label)
        valid.append(label)

    return valid


def create_issue(repo: str, item: dict) -> bool:
    marker = f"<!-- importer-id: {item['importer_id']} -->"
    source_note = f"<!-- imported-from: {ISSUE_FILE} block {item['block_index']} -->"
    final_body = f"{item['body']}\n\n---\n{marker}\n{source_note}"

    with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as tmp:
        tmp.write(final_body)
        tmp_path = tmp.name

    try:
        cmd = [
            "gh", "issue", "create",
            "--repo", repo,
            "--title", item["title"],
            "--body-file", tmp_path,
        ]

        for label in item["labels"]:
            cmd += ["--label", label]

        for assignee in item["assignees"]:
            cmd += ["--assignee", assignee]

        proc = subprocess.run(cmd, text=True, capture_output=True)
        if proc.returncode != 0:
            print(f"ERROR creating issue: {item['title']}")
            print(proc.stdout)
            print(proc.stderr)
            return False

        print(f"Created issue: {item['title']}")
        if proc.stdout.strip():
            print(proc.stdout.strip())
        return True
    finally:
        try:
            os.remove(tmp_path)
        except OSError:
            pass


def main():
    if not REPO:
        print("REPO env var is required.")
        sys.exit(1)

    p = Path(ISSUE_FILE)
    if not p.exists():
        print(f"Issue file not found: {ISSUE_FILE}")
        print("Workflow triggered successfully; nothing to do.")
        return

    text = p.read_text(encoding="utf-8").strip()
    if not text:
        print(f"Issue file is empty: {ISSUE_FILE}")
        return

    items = parse_issue_blocks(text)
    if not items:
        print("No valid issue blocks found.")
        return

    existing_ids = get_existing_importer_ids(REPO)
    known_labels = get_existing_labels(REPO)

    created = 0
    skipped = 0
    failed = 0

    for item in items:
        iid = item["importer_id"]
        title = item["title"]

        if iid in existing_ids:
            print(f"Skip (already imported): {title} [importer-id={iid}]")
            skipped += 1
            continue

        item = dict(item)
        item["labels"] = ensure_labels_exist(REPO, item["labels"], known_labels)

        ok = create_issue(REPO, item)
        if ok:
            existing_ids.add(iid)  # avoid duplicates within same run
            created += 1
        else:
            failed += 1

    print(f"Done. Created={created}, Skipped={skipped}, Failed={failed}, TotalParsed={len(items)}")


if __name__ == "__main__":
    main()