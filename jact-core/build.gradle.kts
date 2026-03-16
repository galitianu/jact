plugins {
    `java-library`
}

dependencies {
    api(project(":jact-annotations"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
