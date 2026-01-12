plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.yellowtale"
version = "1.0.0-RPAL"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.netty:netty-all:4.2.1.Final")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.auth0:java-jwt:4.5.0")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Rubidium-Version" to version,
            "Main-Class" to "com.yellowtale.rubidium.RubidiumPlugin",
            "Implementation-Title" to "Rubidium",
            "Implementation-Version" to version,
            "Description" to "Lithium-style performance optimizations for Hytale servers"
        )
    }
}

tasks.shadowJar {
    archiveBaseName.set("rubidium")
    archiveClassifier.set("")
    
    relocate("com.google.gson", "com.yellowtale.rubidium.libs.gson")
    relocate("org.slf4j", "com.yellowtale.rubidium.libs.slf4j")
    
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
