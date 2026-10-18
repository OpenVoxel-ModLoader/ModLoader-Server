# OpenVoxel ModLoader - dedicated server launcher (Windows PowerShell). Console input stays attached.
$ErrorActionPreference = 'Stop'
$base = Split-Path -Parent $MyInvocation.MyCommand.Path
$settings = @{}
$section = ''
foreach ($raw in Get-Content -LiteralPath (Join-Path $base 'server.properties.yml')) {
    $line = $raw.Trim()
    if (!$line -or $line.StartsWith('#')) { continue }
    if ($line.EndsWith(':') -and $line.IndexOf(':') -eq $line.Length - 1) { $section = $line.TrimEnd(':'); continue }
    $at = $line.IndexOf(':'); if ($at -lt 1) { continue }
    $key = $line.Substring(0,$at).Trim(); $value = $line.Substring($at+1).Trim().Trim("'",'"')
    $settings["$section.$key"] = $value
}
foreach ($d in 'mods','plugins','logs','crash-reports','dist') { New-Item -ItemType Directory -Force -Path (Join-Path $base $d) | Out-Null }
# Server JAR
$jar = $settings['server.jar']
if ($jar) { $jar = Join-Path $base $jar }
if (-not $jar -and $env:OPENVOXEL_SERVER_JAR) { $jar = $env:OPENVOXEL_SERVER_JAR }
foreach ($c in @('server\openvoxel-server.jar', '..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar')) { if (-not $jar -and (Test-Path (Join-Path $base $c))) { $jar = (Resolve-Path (Join-Path $base $c)).Path } }
if (-not $jar -or -not (Test-Path $jar)) {
    Write-Host 'EN: Server JAR not found. Download openvoxel-server-*.jar and set server.jar in server.properties.yml (or copy it to server\openvoxel-server.jar).'
    Write-Host 'ES: No se encontro el JAR del servidor. Descarga openvoxel-server-*.jar y configura server.jar en server.properties.yml (o copialo a server\openvoxel-server.jar).'
    exit 1
}
# Core plugin
$core = Join-Path $base 'dist\ModLoader-v1.1.0-pre3-vgame-0.0.8-server.jar'
if (!(Test-Path $core)) { $env:OPENVOXEL_SERVER_JAR = $jar; & cmd /c (Join-Path $base 'build.bat'); if ($LASTEXITCODE -ne 0) { throw 'Build failed' } }
# Remove other versions of the core plugin so only one ModLoader is loaded
Get-ChildItem (Join-Path $base 'plugins') -Filter 'ModLoader-v*-server.jar' | Where-Object { $_.Name -ne (Split-Path $core -Leaf) } | Remove-Item -Force
Copy-Item $core (Join-Path $base 'plugins') -Force
# mods/ holds server plugins (plugin.yml -> copied to plugins/) and ModLoader mods (openvoxel.mod.properties ->
# read by the ModLoader agent for native blocks). A jar can be both.
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Test-JarEntry($jarPath, $entry) { try { $z = [System.IO.Compression.ZipFile]::OpenRead($jarPath); try { return [bool]($z.Entries | Where-Object { $_.FullName -eq $entry }) } finally { $z.Dispose() } } catch { return $false } }
if ($settings['runtime.auto-deploy-mods'] -eq 'true') {
    Get-ChildItem (Join-Path $base 'mods') -Filter '*.jar' | Where-Object { Test-JarEntry $_.FullName 'plugin.yml' } | Copy-Item -Destination (Join-Path $base 'plugins') -Force
}
$java = $settings['jvm.java']; if (!$java) { $java='java' }
$jvmArgs = @("-Xms$($settings['jvm.min-memory'])","-Xmx$($settings['jvm.max-memory'])")
if ($settings['jvm.use-g1gc'] -eq 'true') { $jvmArgs += '-XX:+UseG1GC' }
# ModLoader agent (native blocks from mods): agent\ModLoader-*-client.jar or OPENVOXEL_MODLOADER_AGENT
$agent = $env:OPENVOXEL_MODLOADER_AGENT
if (-not $agent) { $found = Get-ChildItem (Join-Path $base 'agent') -Filter 'ModLoader-v*-client.jar' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if ($found) { $agent = $found.FullName } }
if ($agent -and (Test-Path $agent)) { $jvmArgs += "-javaagent:$agent=server"; Write-Host "ModLoader agent: $agent (native blocks enabled)" }
else { Write-Host 'ModLoader agent not found in agent\ - native mod blocks are disabled (plugins still work).' }
$serverArgs = @('-jar', $jar, '--port',$settings['server.port'],'--seed',$settings['server.seed'],'--online-mode',$settings['server.online-mode'],'--auth-server',$settings['server.auth-server'])
$configuredServerId = $settings['server.server-id']
if ($configuredServerId -and $configuredServerId -notin @('auto','auto-from-client')) { $serverArgs += @('--server-id',$configuredServerId) }
$log = Join-Path $base 'logs\latest.log'
"[$(Get-Date -Format s)] Starting $jar with $($jvmArgs -join ' ')" | Set-Content -Encoding UTF8 $log
Write-Host "OpenVoxel ModLoader server - output is shown here and saved to logs\latest.log"
Write-Host "Console: help, say <msg>, players, kick <name>, stop   |   Consola: help, say <msg>, players, kick <nombre>, stop"
Push-Location $base
try {
    & $java @jvmArgs @serverArgs 2>&1 | ForEach-Object { $text = $_.ToString(); Write-Host $text; Add-Content -Encoding UTF8 -Path $log -Value $text }
    $code = $LASTEXITCODE
}
catch { $_ | Out-String | Add-Content -Encoding UTF8 $log; $code = 1 }
finally { Pop-Location }
if ($code -ne 0 -and $settings['runtime.crash-reports'] -eq 'true') { "Exit code: $code`nSee: $log" | Set-Content (Join-Path $base 'crash-reports\latest-crash.txt') }
exit $code
