import json
from pathlib import Path
import tempfile
import unittest

from integration_test_shards import allocate, collect_timings, discover_tests, load_timings


class IntegrationTestShardsTest(unittest.TestCase):
    def test_discovers_only_integration_test_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in (
                "example/integration/ExampleTest.kt",
                "example/integration/nested/ExampleIT.kt",
                "example/integration/Helper.kt",
                "example/unit/ExampleTest.kt",
            ):
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.touch()

            self.assertEqual(
                discover_tests(root),
                ["example.integration.ExampleTest", "example.integration.nested.ExampleIT"],
            )

    def test_collects_and_sums_nested_suite_timings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "TEST-one.xml").write_text('<testsuite name="example.ExampleTest" time="1.5"/>')
            (root / "TEST-two.xml").write_text('<testsuite name="example.ExampleTest$Nested" time="2.25"/>')
            (root / "TEST-three.xml").write_text('<testsuite name="example.OtherTest$Nested$Inner" time="3"/>')

            self.assertEqual(collect_timings(root), {"example.ExampleTest": 3.75, "example.OtherTest": 3.0})

    def test_collect_requires_reports_and_valid_durations(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaisesRegex(ValueError, "No JUnit XML"):
                collect_timings(root)
            (root / "TEST-bad.xml").write_text('<testsuite name="example.Test" time="NaN"/>')
            with self.assertRaisesRegex(ValueError, "Invalid test duration"):
                collect_timings(root)

    def test_loads_parts_and_rejects_duplicate_or_invalid_entries(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "one.json").write_text(json.dumps({"example.OneTest": 2}))
            (root / "two.json").write_text(json.dumps({"example.TwoTest": 3.5}))
            self.assertEqual(load_timings(root), {"example.OneTest": 2, "example.TwoTest": 3.5})

            (root / "two.json").write_text(json.dumps({"example.OneTest": 3}))
            with self.assertRaisesRegex(ValueError, "Duplicate timing"):
                load_timings(root)
            (root / "two.json").write_text(json.dumps({"example.TwoTest": True}))
            with self.assertRaisesRegex(ValueError, "Invalid timing"):
                load_timings(root)

    def test_round_robin_without_history(self):
        self.assertEqual(
            allocate(["a", "b", "c", "d", "e"], {}, 2),
            [["a", "c", "e"], ["b", "d"]],
        )

    def test_balances_durations_and_estimates_new_tests(self):
        tests = ["a", "b", "c", "d", "new"]
        timings = {"a": 10, "b": 8, "c": 2, "d": 2}
        shards = allocate(tests, timings, 2)
        self.assertEqual(shards, [["a", "c", "d"], ["b", "new"]])
        self.assertEqual(sorted(test for shard in shards for test in shard), tests)
        self.assertEqual(shards, allocate(tests, timings, 2))

    def test_rejects_invalid_shard_counts_and_duplicate_tests(self):
        for count in (0, 3):
            with self.assertRaises(ValueError):
                allocate(["a", "b"], {}, count)
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            allocate(["a", "a"], {}, 1)

    def test_zero_duration_still_distributes_tests(self):
        shards = allocate(["a", "b", "c"], {"a": 0, "b": 0, "c": 0}, 3)
        self.assertTrue(all(shards))


if __name__ == "__main__":
    unittest.main()
