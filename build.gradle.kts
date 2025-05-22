plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.compose") version "1.5.10"
}

group = "com.github.a2kaido"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.preview)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11) // Compose Desktop requires JVM 11+
}

compose.desktop {
    application {
        mainClass = "com.github.a2kaido.go.MainKt" // Specify the main class
    }
}