plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.yellowtale"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
}

gradlePlugin {
    plugins {
        create("rubidiumPlugin") {
            id = "com.yellowtale.rubidium-plugin"
            implementationClass = "com.yellowtale.rubidium.gradle.RubidiumPluginPlugin"
            displayName = "Rubidium Plugin Gradle Plugin"
            description = "Gradle plugin for building Rubidium server plugins"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
