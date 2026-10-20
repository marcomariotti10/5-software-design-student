#requires -Version 5.1
# autograder.ps1 — Windows PowerShell port of autograder.sh with automatic JAVA_HOME detection

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Join-ExecArgs([string[]]$items) {
  if (-not $items -or $items.Count -eq 0) { return '' }
  $escaped = foreach ($i in $items) {
    if ($i -match '[\s"]') { '"' + ($i -replace '"','\"') + '"' } else { $i }
  }
  return [string]::Join(' ', $escaped)
}

function Get-JdkVersion([string]$jdkHome) {
  $releaseFile = Join-Path $jdkHome 'release'
  if (Test-Path -LiteralPath $releaseFile) {
    $txt = Get-Content -LiteralPath $releaseFile -ErrorAction SilentlyContinue
    $line = $txt | Where-Object { $_ -match 'JAVA_VERSION=' } | Select-Object -First 1
    if ($line -and ($line -match '"(?<v>[0-9]+)')) { return [int]$Matches['v'] }
  }
  $name = Split-Path -Leaf $jdkHome
  if ($name -match '(?<v>1[0-9]|[2-9][0-9])') { return [int]$Matches['v'] }
  return 0
}

function Find-JdkHomes {
  $candidates = @()

  # 1) If javac is on PATH, prefer its home
  $javac = Get-Command javac -ErrorAction SilentlyContinue
  if ($javac) {
    $jdkPath = Split-Path -Parent (Split-Path -Parent $javac.Source)
    if (Test-Path (Join-Path $jdkPath 'bin\javac.exe')) { $candidates += $jdkPath }
  }

  # 2) Registry (Oracle-style)
  $regRoots = @(
    'HKLM:\SOFTWARE\JavaSoft\JDK',
    'HKLM:\SOFTWARE\WOW6432Node\JavaSoft\JDK'
  )
  foreach ($root in $regRoots) {
    if (-not (Test-Path $root)) { continue }
    $cur = (Get-ItemProperty $root -ErrorAction SilentlyContinue).CurrentVersion
    if ($cur) {
      $javaHomePath = (Get-ItemProperty (Join-Path $root $cur) -ErrorAction SilentlyContinue).JavaHome
      if ($javaHomePath -and (Test-Path (Join-Path $javaHomePath 'bin\javac.exe'))) { $candidates += $javaHomePath }
    }
    Get-ChildItem $root -ErrorAction SilentlyContinue | ForEach-Object {
      try {
        $javaHomePath = (Get-ItemProperty $_.PsPath -ErrorAction SilentlyContinue).JavaHome
        if ($javaHomePath -and (Test-Path (Join-Path $javaHomePath 'bin\javac.exe'))) { $candidates += $javaHomePath }
      } catch {}
    }
  }

  # 3) Typical OpenJDK vendor dirs
  $vendorRoots = @(
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Microsoft'),
    (Join-Path $env:ProgramFiles 'AdoptOpenJDK'),
    (Join-Path $env:ProgramFiles 'Zulu')
  ) | Where-Object { $_ -and (Test-Path $_) }

  foreach ($vr in $vendorRoots) {
    Get-ChildItem -LiteralPath $vr -Directory -ErrorAction SilentlyContinue | ForEach-Object {
      if ($_.Name -match '^(jdk|jbr|zulu).*') {
        $jdkPath = $_.FullName
        if (Test-Path (Join-Path $jdkPath 'bin\javac.exe')) { $candidates += $jdkPath }
      }
      Get-ChildItem -LiteralPath $_.FullName -Directory -ErrorAction SilentlyContinue | ForEach-Object {
        $jdkPath = $_.FullName
        if (Test-Path (Join-Path $jdkPath 'bin\javac.exe')) { $candidates += $jdkPath }
      }
    }
  }

  $uniq = $candidates | Sort-Object -Unique
  $scored = $uniq | ForEach-Object {
    $v = Get-JdkVersion $_
    [pscustomobject]@{
      Home    = $_
      Version = $v
      Score   = switch ($v) { 21 {3} 17 {2} default {1} }
      Stamp   = (Get-Item $_).LastWriteTimeUtc
    }
  }
  return $scored | Sort-Object -Property @{e='Score';Descending=$true}, @{e='Version';Descending=$true}, @{e='Stamp';Descending=$true} | Select-Object -ExpandProperty Home
}

function Ensure-JavaHome {
  if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { return $env:JAVA_HOME }

  $homes = Find-JdkHomes
  if ($homes -and $homes.Count -gt 0) {
    $env:JAVA_HOME = $homes[0]
    return $env:JAVA_HOME
  }

  throw "JAVA_HOME is not set and no JDK was found. Install a JDK (Temurin 17 or 21 recommended), then re-run."
}

# --- Script location & repo layout (must be in <repo>\solutions or <repo>\labs) ---
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -LiteralPath $PSCommandPath -Parent }
$BaseDirName = (Split-Path -Leaf $ScriptDir).ToLowerInvariant()

if ($BaseDirName -ne 'solutions' -and $BaseDirName -ne 'labs') {
  Write-Host "[autograder] ERROR: this script must live in either root/solutions or root/labs."
  Write-Host "[autograder] Found at: $ScriptDir (basename: $BaseDirName)"
  exit 1
}

$SolDir  = $ScriptDir
$RootDir = Split-Path -Parent $SolDir

# Prefer the repo root aggregator if present
$AggPom = Join-Path $RootDir 'pom.xml'
if (-not (Test-Path -LiteralPath $AggPom)) { $AggPom = Join-Path $SolDir 'pom.xml' }

# Module selector relative to the AGG_POM location
$rootPomResolved = (Join-Path $RootDir 'pom.xml')
if ((Test-Path -LiteralPath $rootPomResolved) -and
    ((Resolve-Path -LiteralPath $AggPom).Path -ieq (Resolve-Path -LiteralPath $rootPomResolved).Path)) {
  $ModuleSelector = (Split-Path -Leaf $SolDir) + '/grader'
} else {
  $ModuleSelector = 'grader'
}

$ModuleDir = Join-Path $SolDir 'grader'
$ModulePom = Join-Path $ModuleDir 'pom.xml'

$Main = if ($BaseDirName -eq 'solutions') {
  'be.uantwerpen.sd.solutions.grader.GraderMain'
} else {
  'be.uantwerpen.sd.labs.grader.GraderMain'
}

# --- Maven wrapper / mvn detection (prefer wrapper at repo root) ---
$mvnCandidates = @(
  (Join-Path $RootDir 'mvnw.cmd'),
  (Join-Path $RootDir 'mvnw'),
  'mvn.cmd',
  'mvn'
)

$MVN = $null
foreach ($c in $mvnCandidates) {
  if ($c -match '^(mvn(\.cmd)?)$') {
    $cmd = Get-Command $c -ErrorAction SilentlyContinue
    if ($cmd) { $MVN = $cmd.Source; break }
  } else {
    if (Test-Path -LiteralPath $c) { $MVN = (Resolve-Path -LiteralPath $c).Path; break }
  }
}
if (-not $MVN) {
  Write-Host "[autograder] ERROR: Could not find Maven or mvnw(.cmd)." -ForegroundColor Red
  exit 1
}

# --- Ensure JAVA_HOME/JDK on Windows for mvn(.cmd)/mvnw.cmd ---
try {
  $jdkHomeChosen = Ensure-JavaHome
  Write-Host "[autograder] JAVA_HOME=$jdkHomeChosen"
  & (Join-Path $jdkHomeChosen 'bin\javac.exe') -version | Write-Host
} catch {
  Write-Host "[autograder] ERROR: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}

# --- Fresh, isolated Maven local repo each run ---
$RepoDir = Join-Path $RootDir '.m2-autograder'
if (Test-Path -LiteralPath $RepoDir) { Remove-Item -LiteralPath $RepoDir -Recurse -Force }
[void](New-Item -ItemType Directory -Path $RepoDir)

$mvnCommonFlags = @("-Dmaven.repo.local=$RepoDir", "-Dmaven.test.skip=true", "-U")

# --- Logging ---
Write-Host "[autograder] LOCAL_M2=$RepoDir"
Write-Host "[autograder] ROOT_DIR=$RootDir"
Write-Host "[autograder] AGG_POM=$AggPom"
Write-Host "[autograder] MODULE_DIR=$ModuleDir"
Write-Host "[autograder] MAIN=$Main"
Write-Host "[autograder] MODULE_SELECTOR=$ModuleSelector"

Write-Host "[autograder] mvn + java versions:"
try { & $MVN -v } catch { Write-Host $_.Exception.Message }
try { & java -version } catch { Write-Host $_.Exception.Message }

# --- Build reactor (grader + labs) ---
Write-Host "[autograder] building reactor (grader + labs)..."
& $MVN -q -f $AggPom -pl $ModuleSelector -am $mvnCommonFlags install

# --- Normalize "lab" shorthand into GraderMain's --lab forms ---
$forwarded = New-Object System.Collections.Generic.List[string]
$expectLabValue = $false
foreach ($arg in $args) {
  if ($expectLabValue) {
    $forwarded.Add('--lab'); $forwarded.Add($arg)
    $expectLabValue = $false
    continue
  }
  if ($arg -match '^lab=(.+)$') {
    $forwarded.Add("--lab=$($Matches[1])")
  } elseif ($arg -eq 'lab') {
    $expectLabValue = $true
  } else {
    $forwarded.Add($arg)
  }
}
if ($expectLabValue) {
  Write-Host "[autograder] ERROR: 'lab' provided without a value. Usage: lab=lab1 or lab lab1" -ForegroundColor Red
  exit 2
}

# Properly pass program args to exec:java (single string)
$argsJoined = Join-ExecArgs $forwarded.ToArray()
Write-Host "[autograder] exec args: $argsJoined"

# --- Run exec:java from the grader module ---
Push-Location $ModuleDir
try {
  & $MVN -q -f $ModulePom -e $mvnCommonFlags `
    exec:java `
    "-Dexec.classpathScope=runtime" `
    "-Dexec.mainClass=$Main" `
    "-Dexec.workingdir=$ModuleDir" `
    "-Dexec.args=$argsJoined"
} finally {
  Pop-Location
}