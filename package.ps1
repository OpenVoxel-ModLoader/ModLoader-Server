# Builds the ready-to-use server bundle: dist\ModLoader-Server-v1.1.0-pre2-vgame-0.0.8.zip
# Contents: dist\<core jar>, start.bat/.ps1/.sh, server.properties.yml, mods\ plugins\ server\ folders, README, LICENSE.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$jar = 'dist\ModLoader-v1.1.0-pre2-vgame-0.0.8-server.jar'
if (!(Test-Path $jar)) { & cmd /c build.bat; if ($LASTEXITCODE -ne 0) { throw 'Build failed' } }
$stage = Join-Path $env:TEMP 'ModLoader-Server-bundle'
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
$bundle = Join-Path $stage 'ModLoader-Server'
New-Item -ItemType Directory -Force (Join-Path $bundle 'dist') | Out-Null
Copy-Item $jar (Join-Path $bundle 'dist')
foreach ($f in 'start.bat','start.ps1','start.sh','server.properties.yml','README.md','LICENSE') { Copy-Item $f $bundle }
foreach ($d in 'mods','plugins','server','logs') { New-Item -ItemType Directory -Force (Join-Path $bundle $d) | Out-Null }
Set-Content (Join-Path $bundle 'server\PUT-openvoxel-server.jar-HERE.txt') "Download openvoxel-server-0.0.8-SNAPSHOT.jar from open-voxel.net and save it here as openvoxel-server.jar (or set server.jar in server.properties.yml)."
$zip = Join-Path $root 'dist\ModLoader-Server-v1.1.0-pre2-vgame-0.0.8.zip'
if (Test-Path $zip) { Remove-Item $zip }
Compress-Archive -Path $bundle -DestinationPath $zip
Remove-Item -Recurse -Force $stage
Write-Host "Built $zip"
