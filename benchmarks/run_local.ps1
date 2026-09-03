param(
    [ValidateSet("baseline", "optimized")]
    [string]$Label = "baseline",
    [int[]]$Concurrency = @(1, 4, 8),
    [int]$WarmupSeconds = 10,
    [int]$DurationSeconds = 30,
    [int]$Port = 18080
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$resultDirectory = Join-Path $PSScriptRoot ("results\\" + $Label)
New-Item -ItemType Directory -Force -Path $resultDirectory | Out-Null
# The Codex sandbox may expose C:\ as Java's home; keep Maven's cache inside
# the repository so the command has a stable, writable dependency location.
$env:MAVEN_OPTS = "-Duser.home=$root"

$javaVersion = (& cmd /c "java -version 2>&1" | Select-Object -First 1)
$environment = [ordered]@{
    timestamp_utc = [DateTime]::UtcNow.ToString("o")
    os = [Environment]::OSVersion.VersionString
    cpu_logical_processors = [Environment]::ProcessorCount
    memory_bytes = $null # Restricted local shell does not permit querying total physical memory.
    java = $javaVersion
    python = (& python --version 2>&1)
    database = "H2 in-memory 2.x (benchmark Maven profile; MySQL is not measured)"
    server = "Spring Boot embedded Tomcat; max threads 64; Hikari maximum pool 16"
}
$environment | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $resultDirectory "environment.json")

# Remove only output names that this runner owns, so an interrupted attempt
# cannot be mistaken for a complete benchmark run.
$ownedOutputs = @("backend.log", "backend.log.err", "startup.json", "environment.json") +
    ($Concurrency | ForEach-Object { @("library-c$_.json", "resources-c$_.json") })
foreach ($name in $ownedOutputs) {
    Remove-Item -LiteralPath (Join-Path $resultDirectory $name) -Force -ErrorAction SilentlyContinue
}
$environment | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $resultDirectory "environment.json")

$log = Join-Path $resultDirectory "backend.log"
if (Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -InformationLevel Quiet) {
    throw "Port $Port is already in use. Stop the existing service before running a clean benchmark."
}
$server = $null
$startupWatch = [Diagnostics.Stopwatch]::StartNew()
$process = Start-Process -FilePath "mvn.cmd" -ArgumentList @("-Pbenchmark", "spring-boot:run", "-Dspring-boot.run.profiles=benchmark", "-Dspring-boot.run.arguments=--server.port=$Port") -WorkingDirectory $backend -RedirectStandardOutput $log -RedirectStandardError "$log.err" -PassThru -WindowStyle Hidden
try {
    $deadline = (Get-Date).AddSeconds(120)
    do {
        Start-Sleep -Milliseconds 500
        try { $ready = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$Port/api/health" -TimeoutSec 2 } catch { $ready = $null }
    } while (($null -eq $ready -or $ready.StatusCode -ne 200) -and (Get-Date) -lt $deadline)
    if ($null -eq $ready -or $ready.StatusCode -ne 200) { throw "Backend did not become healthy within 120 seconds. See $log" }
    $startupWatch.Stop()
    @{ startup_to_health_seconds = [math]::Round($startupWatch.Elapsed.TotalSeconds, 3) } | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $resultDirectory "startup.json")
    $server = Get-Process -Name java -ErrorAction Stop | Where-Object { $_.StartTime -ge $process.StartTime } | Sort-Object StartTime | Select-Object -Last 1

    foreach ($level in $Concurrency) {
        $out = Join-Path $resultDirectory ("library-c" + $level + ".json")
        $beforeCpu = $server.CPU
        $watch = [Diagnostics.Stopwatch]::StartNew()
        & python (Join-Path $PSScriptRoot "http_benchmark.py") --base-url "http://127.0.0.1:$Port" --concurrency $level --warmup-seconds $WarmupSeconds --duration-seconds $DurationSeconds --output $out
        $watch.Stop()
        if ($LASTEXITCODE -ne 0) { throw "Benchmark at concurrency $level failed." }
        $server.Refresh()
        @{ process_id = $server.Id; measurement_window_seconds = [math]::Round($watch.Elapsed.TotalSeconds, 3); process_cpu_seconds = [math]::Round(($server.CPU - $beforeCpu), 3); process_cpu_utilization_percent = [math]::Round((($server.CPU - $beforeCpu) / ($watch.Elapsed.TotalSeconds * [Environment]::ProcessorCount)) * 100, 3); working_set_bytes_after = $server.WorkingSet64; private_memory_bytes_after = $server.PrivateMemorySize64 } | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $resultDirectory ("resources-c" + $level + ".json"))
    }
}
finally {
    if ($null -ne $server -and !$server.HasExited) { Stop-Process -Id $server.Id -Force }
    if (!$process.HasExited) { Stop-Process -Id $process.Id -Force }
}
