# Reads the per-bundle JaCoCo jacoco.xml artifacts of a Jenkins build on build.servoy.com and reports,
# per bundle, line/branch coverage, the covered lines still needed to reach a target percentage, and
# the classes with the most missed lines.
#
# Authenticates with the same credential as the jenkins MCP: JENKINS_MCP = base64("user:apitoken"),
# sent as Basic auth. Never prompt for or print it.
#
# Usage:
#   pwsh -NoProfile -File jacoco-baseline.ps1 -Job master/servoy-eclipse -BuildNumber 517 -Out coverage-517.json
#   optional: -TargetPct 70 -TopClasses 8
param(
    [Parameter(Mandatory = $true)][string]$Job,
    [Parameter(Mandatory = $true)][int]$BuildNumber,
    [Parameter(Mandatory = $true)][string]$Out,
    [double]$TargetPct = 70,
    [int]$TopClasses = 8
)
$ErrorActionPreference = 'Stop'
if (-not $env:JENKINS_MCP) { throw 'JENKINS_MCP is not set (it is the jenkins MCP credential from opencode.json)' }

# 'master/servoy-eclipse' -> 'job/master/job/servoy-eclipse'
$jobPath = ($Job.Split('/') | ForEach-Object { "job/$_" }) -join '/'
$base = "https://build.servoy.com/jenkins/$jobPath/$BuildNumber"
$headers = @{ Authorization = "Basic $($env:JENKINS_MCP)" }

$buildInfo = Invoke-RestMethod -Uri "$base/api/json?tree=building,result,artifacts[relativePath]" -Headers $headers
"$Job #$BuildNumber building=$($buildInfo.building) result=$($buildInfo.result)"
if ($buildInfo.building) { throw 'The build is still running; artifacts are only archived at the end.' }
$xmls = $buildInfo.artifacts | Where-Object { $_.relativePath -like '*target/site/jacoco/jacoco.xml' }
if (-not $xmls) { throw 'No jacoco.xml artifacts found in this build.' }

function Get-Counter($node, $type) {
    $c = $node.counter | Where-Object { $_.type -eq $type }
    if (-not $c) { return @{ covered = 0; missed = 0 } }
    return @{ covered = [int]$c.covered; missed = [int]$c.missed }
}

function Get-Pct($counter) {
    $t = $counter.covered + $counter.missed
    if ($t -eq 0) { return 0 }
    return [math]::Round(100 * $counter.covered / $t, 1)
}

$result = foreach ($a in $xmls) {
    # the bundle is the path segment before 'target/'
    $parts = $a.relativePath.Split('/')
    $bundle = $parts[[array]::IndexOf($parts, 'target') - 1]
    $raw = Invoke-WebRequest -Uri "$base/artifact/$($a.relativePath)" -Headers $headers
    # jacoco.xml declares a DTD that is not reachable; drop it before parsing
    [xml]$doc = $raw.Content -replace '<!DOCTYPE[^>]*>', ''
    $report = $doc.report
    $line = Get-Counter $report 'LINE'
    $branch = Get-Counter $report 'BRANCH'
    $classes = foreach ($pkg in $report.package) {
        foreach ($cls in $pkg.class) {
            $l = Get-Counter $cls 'LINE'
            [pscustomobject]@{ class = ($cls.name -replace '/', '.'); missedLines = $l.missed; coveredLines = $l.covered }
        }
    }
    $total = $line.covered + $line.missed
    [pscustomobject]@{
        bundle        = $bundle
        lines         = $total
        linePct       = Get-Pct $line
        branchPct     = Get-Pct $branch
        linesToTarget = [math]::Max(0, [math]::Ceiling($TargetPct / 100 * $total) - $line.covered)
        topMissed     = @($classes | Sort-Object missedLines -Descending | Select-Object -First $TopClasses)
    }
}
$result | ConvertTo-Json -Depth 5 | Set-Content -Path $Out -Encoding utf8
$result | Sort-Object bundle | Select-Object bundle, lines, linePct, branchPct, linesToTarget | Format-Table -AutoSize | Out-String -Width 200
"written: $Out"
