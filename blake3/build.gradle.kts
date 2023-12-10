plugins {
    `java-library`
}

version = "0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    testImplementation("commons-codec:commons-codec:1.16.0")
}

tasks.compileJava {
    options.javaModuleVersion.set(provider { version as String })
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
