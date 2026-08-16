#!/usr/bin/env bash

set -euo pipefail

version="${1:-}"
semver_pattern='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-([0-9A-Za-z-]+)(\.[0-9A-Za-z-]+)*)?(\+([0-9A-Za-z-]+)(\.[0-9A-Za-z-]+)*)?$'

if [[ ! "$version" =~ $semver_pattern ]]; then
    echo "Invalid semantic version: $version" >&2
    exit 1
fi

without_build="${version%%+*}"
if [[ "$without_build" == *-* ]]; then
    prerelease="${without_build#*-}"
    IFS='.' read -ra identifiers <<< "$prerelease"
    for identifier in "${identifiers[@]}"; do
        if [[ "$identifier" =~ ^[0-9]+$ && "$identifier" != "0" && "$identifier" == 0* ]]; then
            echo "Invalid semantic version: numeric prerelease identifiers cannot have leading zeros" >&2
            exit 1
        fi
    done
    echo "IS_PRERELEASE=1"
else
    echo "IS_PRERELEASE=0"
fi
