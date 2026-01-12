plugins {
    java
    `java-library`
    `maven-publish`
}

group = "com.yellowtale"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains:annotations:24.0.0")
    api("com.google.code.gson:gson:2.10.1")
    api("org.yaml:snakeyaml:2.2")
    api("com.moandjiezana.toml:toml4j:0.7.2")
    api("org.slf4j:slf4j-api:2.0.9")
    
    implementation("io.netty:netty-all:4.1.100.Final")
    implementation("com.google.guava:guava:32.1.3-jre")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Rubidium Java SDK")
                description.set("Java SDK for Hytale plugin development")
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
