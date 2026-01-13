plugins {
    java
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "com.yellowtale"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

repositories {
    mavenCentral()
    
    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    
    api("org.jetbrains:annotations:24.0.0")
    api("com.google.code.gson:gson:2.11.0")
    api("org.yaml:snakeyaml:2.3")
    api("com.moandjiezana.toml:toml4j:0.7.2")
    api("org.slf4j:slf4j-api:2.0.9")
    
    implementation("io.netty:netty-all:4.1.100.Final")
    implementation("com.google.guava:guava:33.0.0-jre")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.mockito:mockito-core:5.14.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Rubidium Framework",
            "Implementation-Version" to version,
            "Rubidium-Version" to "1.0.0"
        )
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("Rubidium")
    archiveClassifier.set("")
    
    relocate("com.google.gson", "rubidium.libs.gson")
    relocate("org.yaml.snakeyaml", "rubidium.libs.snakeyaml")
    relocate("com.google.common", "rubidium.libs.guava")
    
    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
    }
}

tasks.register("copyToPlugins") {
    dependsOn(tasks.named("shadowJar"))
    doLast {
        val shadowJar = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get()
        val pluginsDir = file("../test-server/plugins")
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
        }
        copy {
            from(shadowJar.archiveFile)
            into(pluginsDir)
        }
        println("Copied ${shadowJar.archiveFileName.get()} to ${pluginsDir.absolutePath}")
    }
}

sourceSets {
    main {
        java {
            srcDirs("src")
        }
        resources {
            srcDirs("resources")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Rubidium Framework")
                description.set("Java SDK for Hytale server plugin development")
                url.set("https://github.com/yellow-tale/rubidium")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("yellowtale")
                        name.set("Yellow Tale Team")
                    }
                }
            }
        }
    }
}
