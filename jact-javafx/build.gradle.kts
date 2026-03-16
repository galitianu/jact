plugins {
    `java-library`
}

val javafxVersion = "21.0.2"
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val javafxPlatform = when {
    osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64")) -> "mac-aarch64"
    osName.contains("mac") -> "mac"
    osName.contains("win") -> "win"
    else -> "linux"
}

dependencies {
    api(project(":jact-core"))
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
}
