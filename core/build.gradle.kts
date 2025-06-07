plugins {
    kotlin("jvm")
}

group = "com.github.a2kaido.go"

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}