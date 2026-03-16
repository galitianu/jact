plugins {
    `java-library`
}

val springBootVersion = "3.3.0"

dependencies {
    api(project(":jact-core"))
    implementation(project(":jact-javafx"))
    implementation(project(":jact-annotations"))

    api("org.springframework.boot:spring-boot-autoconfigure:${springBootVersion}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
