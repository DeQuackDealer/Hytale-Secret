plugins {
    java
}

group = "com.dinomod"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("../../build/libs/Rubidium-1.0.0.jar"))
}

tasks.jar {
    archiveBaseName.set("DinoMod")
    
    manifest {
        attributes(
            "Plugin-Name" to "DinoMod",
            "Plugin-Version" to version,
            "Plugin-Main" to "dinomod.DinoModPlugin",
            "Plugin-Author" to "Rubidium Example",
            "Plugin-Description" to "Example plugin demonstrating all Rubidium APIs"
        )
    }
}
