plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    java
}

dependencies {
    implementation(project(":jact-spring-boot-starter"))
    implementation(project(":jact-annotations"))
    implementation(project(":jact-core"))

    annotationProcessor(project(":jact-compiler"))

    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
