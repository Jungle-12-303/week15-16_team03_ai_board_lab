param(
    [ValidateSet("ensure", "status", "start", "stop", "restart")]
    [string] $Action = "ensure",

    [switch] $OpenBrowser
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend\project-alpha"

$BackendOutLog = Join-Path $BackendDir "bootrun.local-dev.out.log"
$BackendErrLog = Join-Path $BackendDir "bootrun.local-dev.err.log"
$FrontendOutLog = Join-Path $FrontendDir "vite.local-dev.out.log"
$FrontendErrLog = Join-Path $FrontendDir "vite.local-dev.err.log"

function Write-Step {
    param([string] $Message)
    Write-Host "[local-dev] $Message"
}

function Test-CommandExists {
    param([string] $Command)
    return [bool](Get-Command $Command -ErrorAction SilentlyContinue)
}

function Test-TcpPort {
    param(
        [string] $HostName,
        [int] $Port
    )

    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $connection = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $connection.AsyncWaitHandle.WaitOne(1000, $false)

        if ($connected) {
            $client.EndConnect($connection)
        }

        $client.Close()
        return $connected
    } catch {
        return $false
    }
}

function Test-HttpOk {
    param([string] $Url)

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Wait-Until {
    param(
        [scriptblock] $Condition,
        [int] $TimeoutSeconds,
        [string] $WaitingMessage,
        [string] $TimeoutMessage
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        if (& $Condition) {
            return $true
        }

        Write-Step $WaitingMessage
        Start-Sleep -Seconds 2
    }

    Write-Step $TimeoutMessage
    return $false
}

function Start-DockerDesktopIfNeeded {
    if (-not (Test-CommandExists "docker")) {
        throw "docker command was not found. Check Docker Desktop installation and PATH."
    }

    docker info *> $null

    if ($LASTEXITCODE -eq 0) {
        return
    }

    $dockerDesktopCandidates = @(
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
        "${env:ProgramFiles(x86)}\Docker\Docker\Docker Desktop.exe",
        "$env:LocalAppData\Docker\Docker Desktop.exe"
    ) | Where-Object { $_ -and (Test-Path $_) }

    if ($dockerDesktopCandidates.Count -eq 0) {
        throw "Docker daemon is not running, and Docker Desktop executable was not found."
    }

    Write-Step "Docker Desktop is not running. Starting Docker Desktop."
    Start-Process -FilePath $dockerDesktopCandidates[0] -WindowStyle Hidden

    Wait-Until `
        -TimeoutSeconds 90 `
        -WaitingMessage "Waiting for Docker daemon..." `
        -TimeoutMessage "Timed out while waiting for Docker daemon." `
        -Condition {
            docker info *> $null
            return $LASTEXITCODE -eq 0
        } | Out-Null
}

function Start-Infra {
    Start-DockerDesktopIfNeeded

    Write-Step "Checking MySQL/Qdrant Docker Compose services."
    Push-Location $RootDir
    try {
        docker compose up -d mysql qdrant
    } finally {
        Pop-Location
    }

    Wait-Until `
        -TimeoutSeconds 60 `
        -WaitingMessage "Waiting for MySQL port 3306..." `
        -TimeoutMessage "Timed out while waiting for MySQL port." `
        -Condition { Test-TcpPort -HostName "127.0.0.1" -Port 3306 } | Out-Null

    Wait-Until `
        -TimeoutSeconds 60 `
        -WaitingMessage "Waiting for Qdrant port 6333..." `
        -TimeoutMessage "Timed out while waiting for Qdrant port." `
        -Condition { Test-HttpOk -Url "http://127.0.0.1:6333/collections" } | Out-Null
}

function Start-Backend {
    if (Test-HttpOk -Url "http://127.0.0.1:8080/actuator/health") {
        Write-Step "Backend is already running. http://127.0.0.1:8080"
        return
    }

    if (-not (Test-Path (Join-Path $BackendDir "gradlew.bat"))) {
        throw "backend/gradlew.bat was not found."
    }

    Write-Step "Backend is down. Starting Spring Boot."
    Start-Process `
        -FilePath "powershell.exe" `
        -WorkingDirectory $BackendDir `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", ".\gradlew.bat bootRun") `
        -RedirectStandardOutput $BackendOutLog `
        -RedirectStandardError $BackendErrLog `
        -WindowStyle Hidden

    Wait-Until `
        -TimeoutSeconds 120 `
        -WaitingMessage "Waiting for Backend port 8080..." `
        -TimeoutMessage "Timed out while waiting for Backend. Logs: $BackendOutLog, $BackendErrLog" `
        -Condition { Test-HttpOk -Url "http://127.0.0.1:8080/actuator/health" } | Out-Null
}

function Start-Frontend {
    if (Test-HttpOk -Url "http://127.0.0.1:5173") {
        Write-Step "Frontend is already running. http://127.0.0.1:5173"
        return
    }

    if (-not (Test-CommandExists "npm")) {
        throw "npm command was not found. Check Node.js installation and PATH."
    }

    Write-Step "Frontend is down. Starting Vite dev server."
    Start-Process `
        -FilePath "powershell.exe" `
        -WorkingDirectory $FrontendDir `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "npm run dev -- --host 127.0.0.1") `
        -RedirectStandardOutput $FrontendOutLog `
        -RedirectStandardError $FrontendErrLog `
        -WindowStyle Hidden

    Wait-Until `
        -TimeoutSeconds 60 `
        -WaitingMessage "Waiting for Frontend port 5173..." `
        -TimeoutMessage "Timed out while waiting for Frontend. Logs: $FrontendOutLog, $FrontendErrLog" `
        -Condition { Test-HttpOk -Url "http://127.0.0.1:5173" } | Out-Null
}

function Stop-PortProcess {
    param(
        [int] $Port,
        [string] $Name
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($processId in $processIds) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue

        if ($null -ne $process) {
            Write-Step "Stopping $Name process. PID=$processId, Process=$($process.ProcessName)"
            Stop-Process -Id $processId -Force
        }
    }
}

function Stop-LocalDev {
    Stop-PortProcess -Port 5173 -Name "Frontend"
    Stop-PortProcess -Port 8080 -Name "Backend"

    Push-Location $RootDir
    try {
        Write-Step "Stopping MySQL/Qdrant containers."
        docker compose stop mysql qdrant
    } finally {
        Pop-Location
    }
}

function Show-Status {
    $status = @(
        [pscustomobject]@{
            Service = "MySQL"
            Check = "tcp://127.0.0.1:3306"
            Alive = Test-TcpPort -HostName "127.0.0.1" -Port 3306
        },
        [pscustomobject]@{
            Service = "Qdrant"
            Check = "http://127.0.0.1:6333/collections"
            Alive = Test-HttpOk -Url "http://127.0.0.1:6333/collections"
        },
        [pscustomobject]@{
            Service = "Backend"
            Check = "http://127.0.0.1:8080/actuator/health"
            Alive = Test-HttpOk -Url "http://127.0.0.1:8080/actuator/health"
        },
        [pscustomobject]@{
            Service = "Frontend"
            Check = "http://127.0.0.1:5173"
            Alive = Test-HttpOk -Url "http://127.0.0.1:5173"
        }
    )

    $status | Format-Table -AutoSize
}

function Start-LocalDev {
    Start-Infra
    Start-Backend
    Start-Frontend
    Show-Status

    if ($OpenBrowser) {
        Start-Process "http://127.0.0.1:5173"
    }
}

switch ($Action) {
    "status" {
        Show-Status
    }
    "start" {
        Start-LocalDev
    }
    "ensure" {
        Start-LocalDev
    }
    "stop" {
        Stop-LocalDev
        Show-Status
    }
    "restart" {
        Stop-LocalDev
        Start-LocalDev
    }
}
