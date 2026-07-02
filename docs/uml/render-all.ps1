# Render all Mermaid diagrams in docs/uml/ to PDF in docs/report/figures/
# Requires mmdc (mermaid-cli). If not installed globally, runs via npx -y.
#
# Usage (from project root): .\docs\uml\render-all.ps1

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
$umlDir = Join-Path $repoRoot "docs\uml"
$outDir = Join-Path $repoRoot "docs\report\figures"

if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

# Detect mmdc — prefer local (docs/uml/node_modules), then global, then npx fallback
$localMmdc = Join-Path $umlDir "node_modules\.bin\mmdc.cmd"
$mmdcCmd = Get-Command mmdc -ErrorAction SilentlyContinue

if (Test-Path $localMmdc) {
    $mode = "local"
    Write-Output "Using local mmdc ($localMmdc)"
} elseif ($null -ne $mmdcCmd) {
    $mode = "global"
    Write-Output "Using global mmdc"
} else {
    $mode = "npx"
    Write-Output "mmdc not found, using npx --package @mermaid-js/mermaid-cli mmdc"
}

$files = Get-ChildItem -Path $umlDir -Filter "*.mermaid"
foreach ($f in $files) {
    $name = $f.BaseName
    $inFile = $f.FullName
    $outFile = Join-Path $outDir "$name.pdf"
    Write-Output "Rendering $name -> $outFile"
    switch ($mode) {
        "local"  { & $localMmdc -i $inFile -o $outFile -b transparent }
        "global" { & mmdc -i $inFile -o $outFile -b transparent }
        "npx"    { & npx -y --package=@mermaid-js/mermaid-cli -- mmdc -i $inFile -o $outFile -b transparent }
    }
}

Write-Output ""
Write-Output "Done. Output PDFs in: $outDir"
