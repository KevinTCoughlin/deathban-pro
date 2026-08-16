#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
validator="$script_dir/validate-release-version.sh"

assert_valid() {
    local version="$1"
    local expected="$2"
    local actual
    actual="$(bash "$validator" "$version")"
    [[ "$actual" == "IS_PRERELEASE=$expected" ]] || {
        echo "Expected $version prerelease classification to be $expected, got: $actual" >&2
        exit 1
    }
}

assert_invalid() {
    local version="$1"
    if bash "$validator" "$version" >/dev/null 2>&1; then
        echo "Expected invalid semantic version to be rejected: $version" >&2
        exit 1
    fi
}

assert_valid "0.0.0" 0
assert_valid "1.2.3" 0
assert_valid "1.2.3+build.5" 0
assert_valid "1.2.3-alpha" 1
assert_valid "1.2.3-alpha.1" 1
assert_valid "1.2.3-SNAPSHOT+build.5" 1
assert_valid "1.2.3-0" 1

assert_invalid "1.2.3.preview"
assert_invalid "01.2.3"
assert_invalid "1.02.3"
assert_invalid "1.2.03"
assert_invalid "1.2"
assert_invalid "v1.2.3"
assert_invalid "1.2.3-"
assert_invalid "1.2.3+"
assert_invalid "1.2.3-alpha..1"
assert_invalid "1.2.3-alpha_1"
assert_invalid "1.2.3-01"

echo "Release version validation tests passed"
