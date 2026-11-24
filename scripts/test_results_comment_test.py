#!/usr/bin/env python3
"""Tests for the JUnit pull-request comment."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("test_results_comment.py")
SPEC = importlib.util.spec_from_file_location("test_results_comment", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
comment = importlib.util.module_from_spec(SPEC)
sys.modules["test_results_comment"] = comment
SPEC.loader.exec_module(comment)


PASSING = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.OkTest" tests="1">
  <testcase name="works" classname="com.example.OkTest" time="0.01"/>
</testsuite>
"""

MIXED = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.MixedTest" tests="4">
  <testcase name="works" classname="com.example.MixedTest"/>
  <testcase name="fails `badly`\n" classname="com.example.MixedTest">
    <failure message="expected &lt;true&gt; but was &lt;false&gt;" type="AssertionError">not this stack
line 2</failure>
  </testcase>
  <testcase name="explodes" classname="com.example.MixedTest">
    <error message="boom" type="RuntimeException">stack</error>
  </testcase>
  <testcase name="later" classname="com.example.MixedTest">
    <skipped/>
  </testcase>
</testsuite>
"""

NESTED = """<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite tests="1">
    <testcase name="only" classname="com.example.Nested"/>
  </testsuite>
</testsuites>
"""

DOCTYPE = """<?xml version="1.0"?>
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<testsuite><testcase name="nope" classname="com.example.Nope"/></testsuite>
"""


class FakeGitHub:
    def __init__(self, comments=None, pulls=None):
        self.comments = list(comments or [])
        self.pulls = list(pulls or [])
        self.updated = []
        self.deleted = []
        self.created = []
        self._next = 50

    def list_comments(self, pr_number):
        return list(self.comments)

    def create_comment(self, pr_number, body):
        self._next += 1
        created = {
            "id": self._next,
            "body": body,
            "user": {"login": "github-actions[bot]"},
        }
        self.comments.append(created)
        self.created.append(created)
        return created

    def update_comment(self, comment_id, body):
        self.updated.append(comment_id)
        for item in self.comments:
            if item["id"] == comment_id:
                item["body"] = body

    def delete_comment(self, comment_id):
        self.deleted.append(comment_id)
        self.comments = [item for item in self.comments if item["id"] != comment_id]

    def pulls_for_commit(self, sha):
        return list(self.pulls)


def write_tree(files):
    directory = tempfile.TemporaryDirectory()
    root = Path(directory.name)
    for name, contents in files.items():
        path = root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents, encoding="utf-8")
    return directory, root


class SummarizeTests(unittest.TestCase):
    def test_counts_and_escapes_failure_message(self):
        directory, root = write_tree({"one.xml": MIXED, "nested/two.xml": PASSING})
        self.addCleanup(directory.cleanup)
        summary = comment.summarize_directory(root)
        self.assertEqual(summary.tests, 5)
        self.assertEqual(summary.passed, 2)
        self.assertEqual(summary.failed, 1)
        self.assertEqual(summary.errors, 1)
        self.assertEqual(summary.skipped, 1)
        text = comment.render(
            summary,
            run_url="https://github.com/KevinTCoughlin/deathban-pro/actions/runs/1",
            head_sha="abcdef1234567890",
            include_marker=True,
        )
        self.assertIn(comment.MARKER, text)
        self.assertIn("Passed **2**, failed **1**, errors **1**, skipped **1** (5 tests).", text)
        self.assertIn("`com.example.MixedTest.fails 'badly'` (failure):", text)
        self.assertIn("`expected <true> but was <false>`", text)
        self.assertNotIn("not this stack", text)
        self.assertNotIn("\nline 2", text)
        self.assertIn("`abcdef123456`", text)
        self.assertIn("updated in place", text)

    def test_nested_suite_counts_each_case_once(self):
        directory, root = write_tree({"suite.xml": NESTED})
        self.addCleanup(directory.cleanup)
        summary = comment.summarize_directory(root)
        self.assertEqual(summary.tests, 1)
        self.assertEqual(summary.passed, 1)

    def test_missing_directory_and_rejected_xml(self):
        missing = comment.summarize_directory(Path(tempfile.gettempdir()) / "does-not-exist-junit")
        self.assertEqual(missing.files, 0)
        text = comment.render(missing)
        self.assertIn("No JUnit XML was found", text)
        self.assertNotIn(comment.MARKER, text)

        directory, root = write_tree({"bad.xml": DOCTYPE, "broken.xml": "<not-xml"})
        self.addCleanup(directory.cleanup)
        summary = comment.summarize_directory(root)
        self.assertEqual(summary.tests, 0)
        self.assertEqual(summary.skipped_files, 1)
        self.assertEqual(summary.unreadable, 1)
        rendered = comment.render(summary)
        self.assertIn("could not be parsed", rendered)
        self.assertIn("were skipped", rendered)

    def test_hides_untrusted_links_and_caps_failures(self):
        summary = comment.Summary(tests=3, failed=3, files=1)
        summary.failures = [
            comment.Failure(f"com.example.T.case{index}", "x" * 500, "failure")
            for index in range(3)
        ]
        text = comment.render(summary, run_url="javascript:alert(1)", head_sha="not-a-sha")
        self.assertNotIn("javascript:", text)
        self.assertNotIn("not-a-sha", text)
        message = text.split("`x", 1)[1].split("`", 1)[0]
        self.assertLessEqual(len(message), comment.MAX_MESSAGE)
        self.assertNotIn("x" * (comment.MAX_MESSAGE + 1), text)

        many = comment.Summary(tests=25, failed=25, files=1)
        many.failures = [comment.Failure(f"com.example.T.n{index:02d}", "nope", "failure") for index in range(25)]
        listed = comment.render(many)
        self.assertIn("… and 5 more", listed)
        self.assertEqual(listed.count("(failure)"), 20)

    def test_ignores_symlinks(self):
        directory, root = write_tree({"real.xml": PASSING})
        self.addCleanup(directory.cleanup)
        link = root / "link.xml"
        link.symlink_to(root / "real.xml")
        summary = comment.summarize_directory(root)
        self.assertEqual(summary.tests, 1)


class PublishTests(unittest.TestCase):
    def test_creates_then_updates_and_drops_bot_duplicates(self):
        github = FakeGitHub(
            comments=[
                {
                    "id": 3,
                    "body": f"human copy {comment.MARKER}",
                    "user": {"login": "KevinTCoughlin"},
                },
                {
                    "id": 8,
                    "body": f"{comment.MARKER}\nold",
                    "user": {"login": "github-actions[bot]"},
                },
                {
                    "id": 2,
                    "body": f"{comment.MARKER}\nolder",
                    "user": {"login": "github-actions[bot]"},
                },
            ]
        )
        first = comment.publish(github, 86, f"{comment.MARKER}\nnewer\n")
        self.assertEqual(first["action"], "updated")
        self.assertEqual(first["id"], 2)
        self.assertEqual(first["deleted"], [8])
        self.assertEqual(github.created, [])
        self.assertTrue(any(item["id"] == 3 for item in github.comments))

        again = comment.publish(github, 86, f"{comment.MARKER}\nnewer\n")
        self.assertEqual(again["action"], "unchanged")
        self.assertEqual(github.updated, [2])

        fresh = FakeGitHub()
        created = comment.publish(fresh, 86, f"{comment.MARKER}\nfirst\n")
        self.assertEqual(created["action"], "created")
        self.assertEqual(len(fresh.comments), 1)

    def test_choose_pull_request_prefers_open_main(self):
        pulls = [
            {"number": 4, "state": "closed", "base": {"ref": "main"}, "head": {"sha": "abc"}},
            {"number": 9, "state": "open", "base": {"ref": "feature"}, "head": {"sha": "abc"}},
            {"number": 7, "state": "open", "base": {"ref": "main"}, "head": {"sha": "abc"}},
            {"number": 8, "state": "open", "base": {"ref": "main"}, "head": {"sha": "def"}},
        ]
        self.assertEqual(comment.choose_pull_request(pulls, "abc"), 7)
        self.assertIsNone(comment.choose_pull_request(pulls, "missing"))


class WorkflowTests(unittest.TestCase):
    def test_comment_workflows_do_not_grant_flagged_writes(self):
        root = Path(__file__).resolve().parents[1] / ".github" / "workflows"
        for name in ("ci.yml", "pr-test-results.yml"):
            text = (root / name).read_text(encoding="utf-8")
            lowered = text.lower()
            self.assertNotIn("checks:", lowered, name)
            self.assertNotIn("contents: write", lowered, name)
            self.assertNotIn("actions: write", lowered, name)
            self.assertNotIn("security-events:", lowered, name)
            self.assertNotIn("statuses:", lowered, name)
            head, _, _ = text.partition("\njobs:")
            self.assertIn("contents: read", head, name)
            self.assertNotIn("pull-requests:", head, name)
            for line in text.splitlines():
                if "pull-requests:" in line or re_permission_actions(line):
                    self.assertTrue(line.startswith("      "), line)
            for line in text.splitlines():
                stripped = line.strip()
                if stripped.startswith("uses:"):
                    self.assertRegex(stripped, r"@[0-9a-f]{40}( #|$)")
                if stripped.startswith("run:"):
                    self.assertNotIn("${{", stripped)


def re_permission_actions(line: str) -> bool:
    return line.strip().startswith("actions:")


if __name__ == "__main__":
    unittest.main()
