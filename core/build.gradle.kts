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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.register<JavaExec>("run") {
    mainClass.set("com.github.a2kaido.go.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}