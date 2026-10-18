plugins {
    java
}

group = "com.happyuky7.openvoxelmoding"
val loaderVersion = "1.1.0-pre3"
val gameVersion = "0.0.8"
version = loaderVersion

// Compile-only dependency on the dedicated server JAR (OPENVOXEL_SERVER_JAR or a known location).
val serverJar: File = (System.getenv("OPENVOXEL_SERVER_JAR")?.let { file(it) }
    ?: listOf("server/openvoxel-server.jar", "../../../ServerTest/openvoxel-server-0.0.8-SNAPSHOT.jar", "../ServerTest/openvoxel-server-0.0.8-SNAPSHOT.jar")
        .map { projectDir.resolve(it) }.firstOrNull { it.exists() })
    ?: error("Server JAR not found. Set OPENVOXEL_SERVER_JAR=<path to openvoxel-server-0.0.8-SNAPSHOT.jar> or copy it to server/openvoxel-server.jar")

repositories { mavenCentral() }

dependencies { compileOnly(files(serverJar)) }

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(11)
}

tasks.jar {
    archiveFileName.set("ModLoader-v$loaderVersion-vgame-$gameVersion-server.jar")
    destinationDirectory.set(projectDir.resolve("dist"))
    manifest {
        attributes("Implementation-Title" to "OpenVoxel ModLoader (server)", "Implementation-Version" to loaderVersion, "Game-Version" to gameVersion)
    }
}
