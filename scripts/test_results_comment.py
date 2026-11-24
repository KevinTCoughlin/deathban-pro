#!/usr/bin/env python3
"""Summarize Gradle JUnit XML and keep one pull-request comment up to date.

The comment job grants pull-requests: write only. It does not create a check
run, so the workflow never needs checks: write (flagged by Scorecard v5.7).
"""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

MARKER = "<!-- deathban-pro-test-results -->"
BOT_LOGINS = frozenset({"github-actions[bot]"})
MAX_FILE_BYTES = 2_000_000
MAX_FILES = 500
MAX_CASES = 10_000
MAX_STORED_FAILURES = 100
MAX_LISTED = 20
MAX_NAME = 180
MAX_MESSAGE = 240
MAX_BODY = 60_000
API_ROOT = "https://api.github.com"
REPO_PATTERN = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
SHA_PATTERN = re.compile(r"[0-9a-fA-F]{7,40}")
PR_PATTERN = re.compile(r"[1-9][0-9]*")


@dataclass
class Failure:
    name: str
    message: str
    kind: str


@dataclass
class Summary:
    tests: int = 0
    failed: int = 0
    errors: int = 0
    skipped: int = 0
    files: int = 0
    unreadable: int = 0
    skipped_files: int = 0
    truncated: bool = False
    failures: list[Failure] = field(default_factory=list)

    @property
    def passed(self) -> int:
        return self.tests - self.failed - self.errors - self.skipped


class GitHubError(Exception):
    def __init__(self, status: int, body: str) -> None:
        self.status = status
        super().__init__(f"GitHub API {status}: {body[:500]}")


class GitHub:
    def __init__(self, token: str, repository: str) -> None:
        if not REPO_PATTERN.fullmatch(repository):
            raise ValueError(f"invalid repository: {repository}")
        self.token = token
        self.repository = repository

    def request(self, method: str, path: str, payload: dict | None = None):
        data = None
        headers = {
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {self.token}",
            "User-Agent": "deathban-pro-test-results",
            "X-GitHub-Api-Version": "2022-11-28",
        }
        if payload is not None:
            data = json.dumps(payload).encode()
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(
            API_ROOT + path,
            data=data,
            headers=headers,
            method=method,
        )
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                raw = response.read()
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", "replace")
            raise GitHubError(exc.code, detail) from None
        if not raw:
            return None
        return json.loads(raw.decode())

    def list_comments(self, pr_number: int) -> list[dict]:
        comments: list[dict] = []
        for page in range(1, 21):
            batch = self.request(
                "GET",
                f"/repos/{self.repository}/issues/{pr_number}/comments"
                f"?per_page=100&page={page}",
            )
            if batch is None:
                break
            if not isinstance(batch, list):
                raise GitHubError(500, "issue comments did not return a list")
            if not batch:
                break
            comments.extend(batch)
            if len(batch) < 100:
                break
        return comments

    def create_comment(self, pr_number: int, body: str) -> dict:
        created = self.request(
            "POST",
            f"/repos/{self.repository}/issues/{pr_number}/comments",
            {"body": body},
        )
        if not isinstance(created, dict) or "id" not in created:
            raise GitHubError(500, "create comment returned no id")
        return created

    def update_comment(self, comment_id: int, body: str) -> None:
        self.request(
            "PATCH",
            f"/repos/{self.repository}/issues/comments/{comment_id}",
            {"body": body},
        )

    def delete_comment(self, comment_id: int) -> None:
        self.request(
            "DELETE",
            f"/repos/{self.repository}/issues/comments/{comment_id}",
        )

    def pulls_for_commit(self, sha: str) -> list[dict]:
        if not SHA_PATTERN.fullmatch(sha):
            raise ValueError(f"invalid commit sha: {sha}")
        pulls = self.request("GET", f"/repos/{self.repository}/commits/{sha}/pulls")
        if not isinstance(pulls, list):
            raise GitHubError(500, "commits/pulls did not return a list")
        return pulls


def flatten(text: str, limit: int) -> str:
    cleaned: list[str] = []
    for char in text:
        if char in "\r\n\t":
            cleaned.append(" ")
        elif char == "`":
            cleaned.append("'")
        elif char.isprintable():
            cleaned.append(char)
        else:
            cleaned.append(" ")
    squashed = " ".join("".join(cleaned).split())
    if len(squashed) > limit:
        return squashed[: limit - 1] + "…"
    return squashed


def summarize_directory(directory: Path) -> Summary:
    summary = Summary()
    if not directory.is_dir():
        return summary
    paths = sorted(
        path for path in directory.rglob("*.xml") if path.is_file() and not path.is_symlink()
    )
    for path in paths:
        if summary.files >= MAX_FILES or summary.tests >= MAX_CASES:
            summary.truncated = True
            summary.skipped_files += 1
            continue
        summary.files += 1
        parse_file(path, summary)
    summary.failures.sort(key=lambda item: (item.name, item.kind, item.message))
    return summary


def parse_file(path: Path, summary: Summary) -> None:
    try:
        data = path.read_bytes()
    except OSError:
        summary.unreadable += 1
        return
    if len(data) > MAX_FILE_BYTES:
        summary.skipped_files += 1
        summary.files -= 1
        return
    folded = data.upper()
    if b"<!DOCTYPE" in folded or b"<!ENTITY" in folded:
        summary.skipped_files += 1
        summary.files -= 1
        return
    try:
        root = ET.fromstring(data)
    except ET.ParseError:
        summary.unreadable += 1
        return
    for case in root.iter("testcase"):
        if summary.tests >= MAX_CASES:
            summary.truncated = True
            return
        record_case(case, summary)


def record_case(case: ET.Element, summary: Summary) -> None:
    summary.tests += 1
    failure = case.find("failure")
    error = case.find("error")
    skipped = case.find("skipped")
    if failure is not None:
        summary.failed += 1
        remember(case, failure, "failure", summary)
    elif error is not None:
        summary.errors += 1
        remember(case, error, "error", summary)
    elif skipped is not None:
        summary.skipped += 1


def remember(case: ET.Element, node: ET.Element, kind: str, summary: Summary) -> None:
    if len(summary.failures) >= MAX_STORED_FAILURES:
        summary.truncated = True
        return
    classname = (case.get("classname") or "").strip()
    name = (case.get("name") or "").strip()
    if classname and name:
        display = f"{classname}.{name}"
    else:
        display = classname or name or "(unnamed)"
    message = (node.get("message") or "").strip()
    if not message:
        message = (node.text or "").strip().splitlines()[0].strip() if (node.text or "").strip() else ""
    summary.failures.append(Failure(display, message[:500], kind))


def valid_run_url(url: str | None) -> bool:
    if not url or not url.startswith("https://"):
        return False
    return not any(char.isspace() or char in "()" for char in url)


def render(
    summary: Summary,
    *,
    run_url: str | None = None,
    head_sha: str | None = None,
    include_marker: bool = False,
) -> str:
    lines: list[str] = []
    if include_marker:
        lines.extend([MARKER, ""])
    lines.extend(["## Test results", ""])
    if (
        summary.tests == 0
        and summary.files == 0
        and summary.unreadable == 0
        and summary.skipped_files == 0
    ):
        lines.extend(
            [
                "No JUnit XML was found under `build/test-results/test`.",
                "",
            ]
        )
    else:
        lines.extend(
            [
                (
                    f"Passed **{summary.passed}**, failed **{summary.failed}**, "
                    f"errors **{summary.errors}**, skipped **{summary.skipped}** "
                    f"({summary.tests} tests)."
                ),
                "",
            ]
        )
        if summary.failed or summary.errors:
            lines.extend(["### Failures", ""])
            shown = summary.failures[:MAX_LISTED]
            for item in shown:
                label = "error" if item.kind == "error" else "failure"
                name = flatten(item.name, MAX_NAME)
                message = flatten(item.message, MAX_MESSAGE)
                if message:
                    lines.append(f"- `{name}` ({label}): `{message}`")
                else:
                    lines.append(f"- `{name}` ({label})")
            hidden = summary.failed + summary.errors - len(shown)
            if hidden > 0:
                lines.append(f"- … and {hidden} more")
            lines.append("")
        notes: list[str] = []
        if summary.unreadable:
            notes.append(f"{summary.unreadable} file(s) could not be parsed")
        if summary.skipped_files:
            notes.append(
                f"{summary.skipped_files} file(s) were skipped "
                "(too large, too many, or not plain XML)"
            )
        if summary.truncated:
            notes.append("the result list was truncated")
        if notes:
            lines.extend([f"Notes: {'; '.join(notes)}.", ""])
    footer: list[str] = []
    if valid_run_url(run_url):
        footer.append(f"[CI run]({run_url})")
    if head_sha and SHA_PATTERN.fullmatch(head_sha):
        footer.append(f"`{head_sha[:12]}`")
    if footer:
        lines.extend([" · ".join(footer), ""])
    if include_marker:
        lines.extend(["This comment is updated in place on each push.", ""])
    text = "\n".join(lines).rstrip() + "\n"
    if len(text) > MAX_BODY:
        text = text[: MAX_BODY - 80].rstrip() + "\n\n(comment truncated)\n"
    return text


def plan_comment_update(comments: list[dict], marker: str) -> dict:
    matching = []
    for comment in comments:
        author = ((comment.get("user") or {}).get("login")) or ""
        if marker in (comment.get("body") or "") and author in BOT_LOGINS:
            matching.append(comment)
    matching.sort(key=lambda comment: comment["id"])
    if not matching:
        return {"update": None, "delete": []}
    return {
        "update": matching[0]["id"],
        "delete": [comment["id"] for comment in matching[1:]],
    }


def choose_pull_request(pulls: list[dict], head_sha: str | None) -> int | None:
    opened = [pull for pull in pulls if pull.get("state") == "open"]
    if head_sha:
        opened = [
            pull for pull in opened if ((pull.get("head") or {}).get("sha")) == head_sha
        ]
    if not opened:
        return None
    onto_main = [pull for pull in opened if ((pull.get("base") or {}).get("ref")) == "main"]
    pool = onto_main or opened
    pool.sort(key=lambda pull: pull.get("number") or 0)
    number = pool[0].get("number")
    return int(number) if isinstance(number, int) else None


def publish(github: GitHub, pr_number: int, body: str, marker: str = MARKER) -> dict:
    comments = github.list_comments(pr_number)
    plan = plan_comment_update(comments, marker)
    if plan["update"] is None:
        created = github.create_comment(pr_number, body)
        action = "created"
        comment_id = created["id"]
    else:
        current = next(comment for comment in comments if comment["id"] == plan["update"])
        if (current.get("body") or "").strip() != body.strip():
            github.update_comment(plan["update"], body)
            action = "updated"
        else:
            action = "unchanged"
        comment_id = plan["update"]
    for extra in plan["delete"]:
        github.delete_comment(extra)
    return {"action": action, "id": comment_id, "deleted": plan["delete"]}


def write_step_summary(text: str) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as handle:
        handle.write(text)
        if not text.endswith("\n"):
            handle.write("\n")


def context_from_env() -> tuple[str | None, str | None]:
    run_url = os.environ.get("RUN_URL") or None
    head_sha = os.environ.get("HEAD_SHA") or None
    return run_url, head_sha


def command_summarize(argv: list[str]) -> int:
    if len(argv) > 2:
        directory = Path(argv[2])
    else:
        directory = Path(os.environ.get("RESULTS_DIR", "build/test-results/test"))
    run_url, head_sha = context_from_env()
    sys.stdout.write(render(summarize_directory(directory), run_url=run_url, head_sha=head_sha))
    return 0


def command_post() -> int:
    token = os.environ.get("GITHUB_TOKEN") or ""
    repository = os.environ.get("GITHUB_REPOSITORY") or ""
    if not token or not repository:
        print("GITHUB_TOKEN and GITHUB_REPOSITORY are required", file=sys.stderr)
        return 2
    directory = Path(os.environ.get("RESULTS_DIR", "junit-xml"))
    run_url, head_sha = context_from_env()
    summary = summarize_directory(directory)
    body = render(summary, run_url=run_url, head_sha=head_sha, include_marker=True)
    write_step_summary(body)
    github = GitHub(token, repository)
    pr_raw = os.environ.get("PR_NUMBER") or ""
    if pr_raw:
        if not PR_PATTERN.fullmatch(pr_raw):
            print(f"invalid PR_NUMBER: {pr_raw}", file=sys.stderr)
            return 2
        pr_number = int(pr_raw)
    else:
        if not head_sha or not SHA_PATTERN.fullmatch(head_sha):
            print("PR_NUMBER or a valid HEAD_SHA is required", file=sys.stderr)
            return 2
        pr_number = choose_pull_request(github.pulls_for_commit(head_sha), head_sha)
        if pr_number is None:
            print(f"No open pull request for {head_sha}", file=sys.stderr)
            return 0
    result = publish(github, pr_number, body)
    print(
        f"{result['action']} comment {result['id']} on pull request {pr_number}"
        + (f" (removed {len(result['deleted'])} duplicate(s))" if result["deleted"] else "")
    )
    build_result = os.environ.get("BUILD_RESULT") or ""
    if (
        summary.tests == 0
        and summary.files == 0
        and summary.unreadable == 0
        and summary.skipped_files == 0
        and build_result == "success"
    ):
        print("Build succeeded but no JUnit XML was downloaded", file=sys.stderr)
        return 1
    return 0


def main(argv: list[str]) -> int:
    if len(argv) < 2 or argv[1] not in {"summarize", "post"}:
        print(
            "usage: test_results_comment.py summarize [DIR] | post",
            file=sys.stderr,
        )
        return 2
    if argv[1] == "summarize":
        return command_summarize(argv)
    return command_post()


if __name__ == "__main__":
    sys.exit(main(sys.argv))
