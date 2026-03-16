plugins {
    `java-library`
}

dependencies {
    implementation(project(":jact-annotations"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.google.testing.compile:compile-testing:0.21.0")
}
