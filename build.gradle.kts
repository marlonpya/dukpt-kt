plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "io.github.marlonpya"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.marlonpya.dukpt.DemoKt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
