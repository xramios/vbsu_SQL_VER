#!/usr/bin/env powershell
# Setup and run script for real_seeder on Windows

$ErrorActionPreference = "Stop"

# Track if we auto-detected JAVA_HOME to alert user later
$javaHomeAutoDetected = $false

# Check if JAVA_HOME is set
Write-Host "Checking for Java installation..." -ForegroundColor Cyan
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME not set. Searching for Java installation..." -ForegroundColor Yellow

    # Common Java installation paths to check
    $javaPaths = @(
        "C:\Program Files\Java\jdk*"
        "C:\Program Files\Java\jre*"
        "C:\Program Files (x86)\Java\jdk*"
        "C:\Program Files (x86)\Java\jre*"
        "C:\Program Files\Eclipse Adoptium\jdk*"
        "C:\Program Files\Amazon Corretto\jdk*"
        "C:\Program Files\Microsoft\jdk*"
    )

    $foundJava = $null
    foreach ($path in $javaPaths) {
        $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $foundJava = $found.FullName
            break
        }
    }

    if ($foundJava) {
        Write-Host "Found Java at: $foundJava" -ForegroundColor Green
        $env:JAVA_HOME = $foundJava
        $javaHomeAutoDetected = $true
    } else {
        Write-Host "ERROR: Could not find Java installation." -ForegroundColor Red
        Write-Host "Please install Java 17 or higher and set JAVA_HOME environment variable." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "JAVA_HOME is set to: $env:JAVA_HOME" -ForegroundColor Green
}

# Check if Python exists
Write-Host "Checking for Python installation..." -ForegroundColor Cyan
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
    Write-Host "ERROR: Python is not installed or not in PATH." -ForegroundColor Red
    Write-Host "Please install Python 3.11 or higher from https://www.python.org/downloads/" -ForegroundColor Yellow
    exit 1
}

$pythonVersion = python --version 2>&1
Write-Host "Found Python: $pythonVersion" -ForegroundColor Green

# Get the script directory
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$scriptsDir = Join-Path $projectRoot "scripts"
$requirementsPath = Join-Path $scriptsDir "requirements.txt"

# Check if requirements.txt exists
if (-not (Test-Path $requirementsPath)) {
    Write-Host "ERROR: requirements.txt not found at $requirementsPath" -ForegroundColor Red
    exit 1
}

# Install dependencies
Write-Host "`nInstalling dependencies from requirements.txt..." -ForegroundColor Cyan
try {
    pip install -r $requirementsPath
    if ($LASTEXITCODE -ne 0) {
        throw "pip install failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "ERROR: Failed to install dependencies" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host "`nDependencies installed successfully!" -ForegroundColor Green

# Navigate to scripts directory and run the seeder
Write-Host "`nRunning real_seeder..." -ForegroundColor Cyan
Set-Location $scriptsDir

try {
    if ($env:MYSQL) {
        python -m real_seeder.cli --db-type mysql --database sample --user root --password password 
    } else {
        python -m real_seeder.cli --db-type derby --database sample --user app --password app
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Seeder failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "ERROR: Seeder execution failed" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host "`nSeeder completed successfully!" -ForegroundColor Green

# Alert user about JAVA_HOME if it was auto-detected
if ($javaHomeAutoDetected) {
    Write-Host "`n=================================================================" -ForegroundColor Yellow
    Write-Host "NOTE: JAVA_HOME was auto-detected for this session only." -ForegroundColor Yellow
    Write-Host "To avoid this check in the future, please set JAVA_HOME permanently:" -ForegroundColor Yellow
    Write-Host "  1. Open System Properties > Environment Variables" -ForegroundColor Yellow
    Write-Host "  2. Add a new System Variable named JAVA_HOME" -ForegroundColor Yellow
    Write-Host "  3. Set the value to: $env:JAVA_HOME" -ForegroundColor Yellow
    Write-Host "=================================================================" -ForegroundColor Yellow
}
