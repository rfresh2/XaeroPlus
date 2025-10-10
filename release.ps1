param (
    [Parameter(Mandatory=$true)][string]$headRef,
    [Parameter(Mandatory=$true)][string]$baseRef,
    [string]$repo = "rfresh2/XaeroPlus",
    [string]$workflow = "release.yml",
    [string]$mainBranch = "1.20.1",
    [switch]$releaseAllBranches = $true
)

$branches = @(
    "1.19.2",
    "1.19.4",
    "1.20.1",
    "1.20.2",
    "1.20.4",
    "1.20.6",
    "1.21",
    "1.21.3",
    "1.21.4",
    "1.21.5",
    "1.21.8",
    "1.21.9
)

gh workflow run $workflow --repo $repo --ref $mainBranch -f headRef=$headRef -f baseRef=$baseRef -f githubReleaseRef=$mainBranch

Write-Host "Release workflow requested, waiting for it to appear on runs list..."
Start-Sleep -Seconds 30

$runId = gh run list --repo $repo --workflow $workflow --json databaseId -q '.[0].databaseId'
Write-Host "Found release workflow ID: $runId"

gh run watch $runId --repo $repo --compact --exit-status

if ($LASTEXITCODE -ne 0) {
    Write-Host "Release workflow failed on main branch."
    exit $LASTEXITCODE
}

$childBranches = @()
if ($releaseAllBranches) {
    Write-Host "Releasing to all branches..."
    $childBranches = $branches | Where-Object { $_ -ne $mainBranch }

    foreach ($ref in $childBranches) {
        gh workflow run $workflow --repo $repo --ref $ref -f headRef=$headRef -f baseRef=$baseRef -f githubReleaseRef=$mainBranch
        Write-Host "Release workflow started on branch: $ref"
    }
}
