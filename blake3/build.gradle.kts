plugins {
    `java-library`
}

version = "0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    testImplementation("commons-codec:commons-codec:1.15")
}

tasks.compileJava {
    options.javaModuleVersion.set(provider { version as String })
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
