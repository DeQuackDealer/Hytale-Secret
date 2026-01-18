plugins {
    java
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    
    flatDir {
        dirs("../../build/libs")
    }
}

dependencies {
    compileOnly(files("../../build/libs/Rubidium-1.0.0.jar"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(sourceSets.main.get().output)
    
    manifest {
        attributes(
            "Implementation-Title" to "Joke Mod",
            "Implementation-Version" to version
        )
    }
    
    archiveBaseName.set("JokeMod")
    archiveVersion.set("1.0.0")
}
