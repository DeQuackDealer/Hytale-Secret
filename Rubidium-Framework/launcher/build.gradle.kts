plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
    }
}

application {
    mainClass.set("launcher.TestServerLauncher")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "launcher.TestServerLauncher",
            "Implementation-Title" to "Rubidium Test Server Launcher",
            "Implementation-Version" to "1.0.0"
        )
    }
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("TestServerLauncher")
    archiveClassifier.set("")
    
    manifest {
        attributes("Main-Class" to "launcher.TestServerLauncher")
    }
    
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
