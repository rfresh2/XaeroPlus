#!/usr/bin/env bash
set -euo pipefail

repo="rfresh2/XaeroPlus"
workflow="release.yml"
mainBranch="1.20.1"
releaseAllBranches=true

branches=(
    "1.19.2"
#    "1.19.4"
    "1.20.1"
#    "1.20.2"
    "1.20.4"
#    "1.20.6"
    "1.21"
#    "1.21.3"
    "1.21.4"
    "1.21.5"
    "1.21.8"
    "1.21.10"
    "1.21.11"
)

headRef=""
baseRef=""

usage() {
    echo "Usage: $0 --headRef <ref> --baseRef <ref> [options]"
    echo ""
    echo "Options:"
    echo "  --headRef <ref>             Required. Head commit hash for changelog generator."
    echo "  --baseRef <ref>             Required. Base commit has for changelog generator."
    echo "  --repo <owner/repo>         GitHub repository (default: $repo)"
    echo "  --workflow <file>           Workflow file (default: $workflow)"
    echo "  --mainBranch <branch>       Main release branch (default: $mainBranch)"
    echo "  --releaseAllBranches <bool> Whether to trigger all branches (default: $releaseAllBranches)"
    echo "  -h, --help                  Show this message"
    echo ""
    echo "Example:"
    echo "  $0 --headRef 6964e3a6c789744c1dcf6d569a03c9495a29a208 --baseRef 1680f4126b59069e51b910c5b6fc90b39e6febca"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --headRef)
            headRef="$2"
            shift 2
            ;;
        --baseRef)
            baseRef="$2"
            shift 2
            ;;
        --repo)
            repo="$2"
            shift 2
            ;;
        --workflow)
            workflow="$2"
            shift 2
            ;;
        --mainBranch)
            mainBranch="$2"
            shift 2
            ;;
        --releaseAllBranches)
            releaseAllBranches="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

if [[ -z "$headRef" || -z "$baseRef" ]]; then
    echo "❌ Error: --headRef and --baseRef are required."
    usage
fi

echo "Triggering release workflow on main branch: $mainBranch"
gh workflow run "$workflow" \
    --repo "$repo" \
    --ref "$mainBranch" \
    -f headRef="$headRef" \
    -f baseRef="$baseRef" \
    -f githubReleaseRef="$mainBranch"

echo "Release workflow requested, waiting for it to appear on runs list..."
sleep 30

runId=$(gh run list \
    --repo "$repo" \
    --workflow "$workflow" \
    --json databaseId \
    --jq '.[0].databaseId')

echo "Found release workflow ID: $runId"

gh run watch "$runId" --repo "$repo" --compact --exit-status
status=$?

if [[ $status -ne 0 ]]; then
    echo "❌ Release workflow failed on main branch."
    exit "$status"
fi

if [[ "$releaseAllBranches" == "true" ]]; then
    echo "Releasing to all branches..."
    for ref in "${branches[@]}"; do
        if [[ "$ref" != "$mainBranch" ]]; then
            echo "Starting release workflow on branch: $ref"
            gh workflow run "$workflow" \
                --repo "$repo" \
                --ref "$ref" \
                -f headRef="$headRef" \
                -f baseRef="$baseRef" \
                -f githubReleaseRef="$mainBranch"
        fi
    done
fi
