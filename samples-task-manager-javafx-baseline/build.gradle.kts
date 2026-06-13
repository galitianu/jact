plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}

val javafxVersion = "21.0.2"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("io.jact.baseline.TaskManagerBaselineLauncher")
}
