# OpenVoxel ModLoader — server

[English](#english) · [Español](#español)

Part of [github.com/OpenVoxel-ModLoader](https://github.com/OpenVoxel-ModLoader):
[ModLoader-Client](https://github.com/OpenVoxel-ModLoader/ModLoader-Client) · **ModLoader-Server** (this repo) ·
[docs](https://github.com/OpenVoxel-ModLoader/openvoxel-modloader.github.io) · [ModLoader-ModsExamples](https://github.com/OpenVoxel-ModLoader/ModLoader-ModsExamples) · [ModLoader-Server-Example](https://github.com/OpenVoxel-ModLoader/ModLoader-Server-Example)

## English

Runs the official OpenVoxel dedicated server unmodified, with the **ModLoader server core**
plugin (`ModLoader-v1.1.0-pre2-vgame-0.0.8-server.jar`) and any plugins you drop in `mods/`.
Server plugins use the official plugin API (`net.openvoxel.server.plugin.*`).

### Run

1. Download `openvoxel-server-0.0.8-SNAPSHOT.jar` from open-voxel.net and either copy it to
   `server/openvoxel-server.jar` or set `server.jar` in `server.properties.yml`.
2. Download the core jar from Releases into `dist/` (or it is built on first start).
3. `start.bat` (Windows), `start.ps1`, or `./start.sh` (Linux/macOS). Output is shown live and
   logged to `logs/latest.log`; the window is an interactive console (`help`, `say`, `players`,
   `kick`, `stop`).
4. Settings: `server.properties.yml` (port, seed, online mode, memory...). Plugins: `mods/*.jar`
   are copied to `plugins/` on start.

### Core plugin

Console commands from stdin, `/help` with colors, `/say`, `/list`, `/players`, `/heal`, `/kick`,
`/stop`, `/platform`, `/modtest`, `/modreload`, welcome message (`plugins/ModLoaderServer/config.yml`).

### Examples

[ModLoader-Server-Example](https://github.com/OpenVoxel-ModLoader/ModLoader-Server-Example) — Hunger Games mini-game (`/hg start`).

### Build

`./gradlew jar` or `build.bat` / `build.sh` (needs the server JAR: `OPENVOXEL_SERVER_JAR`).

## Español

Ejecuta el servidor dedicado oficial de OpenVoxel sin modificar, con el plugin **núcleo del
ModLoader** (`ModLoader-v1.1.0-pre2-vgame-0.0.8-server.jar`) y los plugins que pongas en `mods/`.
Los plugins de servidor usan la API oficial (`net.openvoxel.server.plugin.*`).

### Ejecutar

1. Descarga `openvoxel-server-0.0.8-SNAPSHOT.jar` de open-voxel.net y cópialo a
   `server/openvoxel-server.jar` o indica su ruta en `server.jar` de `server.properties.yml`.
2. Descarga el jar del núcleo de Releases a `dist/` (o se compila al arrancar).
3. `start.bat` (Windows), `start.ps1` o `./start.sh` (Linux/macOS). La salida se ve en vivo y se
   guarda en `logs/latest.log`; la ventana es una consola interactiva (`help`, `say`, `players`,
   `kick`, `stop`).
4. Ajustes: `server.properties.yml`. Plugins: los `mods/*.jar` se copian a `plugins/` al arrancar.

### Compilar

`./gradlew jar` o `build.bat` / `build.sh` (necesita el JAR del servidor: `OPENVOXEL_SERVER_JAR`).
