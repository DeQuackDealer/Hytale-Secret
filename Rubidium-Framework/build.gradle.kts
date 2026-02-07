plugins {
    java
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.rubidium"
version = "1.0"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:all",
        "-Xlint:-processing"
    ))
}

repositories {
    mavenCentral()
    
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // HytaleServer.jar is optional - only include if present for development
    val hytaleServerJar = file("libs/HytaleServer.jar")
    if (hytaleServerJar.exists()) {
        compileOnly(files(hytaleServerJar))
    }
    
    api("org.jetbrains:annotations:24.0.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
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

tasks.withType<Javadoc> {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("source", "25")
    opts.addStringOption("Xdoclint:none", "-quiet")
    isFailOnError = false
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Rubidium Framework",
            "Implementation-Version" to version,
            "Rubidium-Version" to "1.0"
        )
    }
}

// Common shadow configuration for both editions
fun com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.configureCommon(tier: String, isPremium: Boolean) {
    // CRITICAL: Exclude development stubs - they would conflict with real Hytale classes at runtime
    exclude("com/hypixel/**")
    
    // Relocate ALL bundled dependencies to avoid conflicts with server/other plugins
    relocate("com.google", "rubidium.libs.google")
    relocate("org.yaml.snakeyaml", "rubidium.libs.snakeyaml")
    relocate("com.moandjiezana.toml", "rubidium.libs.toml")
    relocate("io.netty", "rubidium.libs.netty")
    relocate("org.slf4j", "rubidium.libs.slf4j")
    relocate("org.checkerframework", "rubidium.libs.checkerframework")
    // javax.annotation is provided by HytaleServer.jar at runtime, don't relocate
    
    // Exclude compile-only annotations (not needed at runtime)
    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("com.google.code.findbugs:jsr305"))
    }
    
    // Merge service files for proper SPI loading
    mergeServiceFiles()
    
    // Set manifest for plugin loading
    manifest {
        attributes(
            "Implementation-Title" to "Rubidium Framework",
            "Implementation-Version" to project.version,
            "Rubidium-Version" to "1.0",
            "Rubidium-Tier" to tier,
            "Rubidium-Premium" to isPremium.toString()
        )
    }
}

// FREE Edition - rubidium.jar
// Includes: Optimization, Plugin system, Command/Chat/Event/Config APIs
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("rubidiumFreeJar") {
    archiveBaseName.set("rubidium")
    archiveClassifier.set("")
    archiveVersion.set("")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    
    configureCommon("FREE", false)
    
    // Include manifest.json at root for Hytale mod loading (singular, not plural!)
    from("resources/manifest.json")
    
    // Exclude old files and Plus-specific manifest
    exclude("manifests.json")
    exclude("manifests_plus.json")
    exclude("manifest_plus.json")
    exclude("plugin.json")
    exclude("plugin_plus.json")
    
    // Exclude Plus-only features from FREE edition
    // Use correct package paths matching actual source structure
    exclude("rubidium/voicechat/**")
    exclude("rubidium/minimap/**")
    exclude("rubidium/stats/**")
    exclude("rubidium/hud/**")
    exclude("rubidium/admin/**")
    exclude("rubidium/replay/**")
    exclude("rubidium/api/npc/**")
    exclude("rubidium/api/ai/**")
    exclude("rubidium/api/pathfinding/**")
    exclude("rubidium/api/worldgen/**")
    exclude("rubidium/api/inventory/**")
    exclude("rubidium/api/economy/**")
    exclude("rubidium/api/particles/**")
    exclude("rubidium/api/bossbar/**")
    exclude("rubidium/api/scoreboard/**")
    exclude("rubidium/hytale/ui/**")
    // NOTE: Do NOT exclude rubidium/hytale/adapter/** - it contains PlayerEventHandler
    // which is needed for core event handling in both editions
}

// PLUS Edition - rubidium_plus.jar
// Includes: Everything
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("rubidiumPlusJar") {
    archiveBaseName.set("rubidium_plus")
    archiveClassifier.set("")
    archiveVersion.set("")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    
    configureCommon("PLUS", true)
    
    // Include manifest.json at root for Hytale mod loading (singular, not plural!)
    from("resources/manifest_plus.json") {
        rename("manifest_plus.json", "manifest.json")
    }
    
    // Exclude old files and non-plus manifest
    exclude("manifests.json")
    exclude("manifests_plus.json")
    exclude("plugin.json")
    exclude("plugin_plus.json")
}

// Default shadowJar builds Plus edition
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("Rubidium")
    archiveClassifier.set("")
    
    configureCommon("PLUS", true)
}

// Build both editions
tasks.register("buildAllEditions") {
    dependsOn("rubidiumFreeJar", "rubidiumPlusJar")
    doLast {
        println("Built both Rubidium editions:")
        println("  - build/libs/rubidium.jar (Free Edition)")
        println("  - build/libs/rubidium_plus.jar (Plus Edition)")
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

tasks.register<Jar>("launcherJar") {
    dependsOn(tasks.named("compileLauncherJava"))
    
    archiveBaseName.set("TestServerLauncher")
    archiveClassifier.set("")
    archiveVersion.set("")
    
    from(sourceSets["launcher"].output)
    
    manifest {
        attributes("Main-Class" to "launcher.TestServerLauncher")
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("copyTestScripts") {
    from("scripts")
    into(layout.buildDirectory.dir("libs"))
    include("*.sh", "*.bat", "*.ps1")
}

tasks.named("assemble") {
    dependsOn(tasks.named("launcherJar"), tasks.named("copyTestScripts"))
}

tasks.register("buildAll") {
    dependsOn(tasks.named("assemble"))
    doLast {
        println("Built artifacts:")
        println("  - build/libs/Rubidium-${version}.jar (Framework)")
        println("  - build/libs/TestServerLauncher.jar (Launcher GUI)")
        println("  - build/libs/rubidium-test-gui.sh (Test Script)")
    }
}

sourceSets {
    main {
        java {
            srcDirs("src")
            exclude("launcher/**")
        }
        resources {
            srcDirs("resources")
        }
    }
    create("launcher") {
        java {
            srcDirs("src/launcher")
        }
    }
}

// Development JAR with stubs included (for testing outside of Hytale)
tasks.register<Jar>("rubidiumDevJar") {
    archiveBaseName.set("rubidium-dev")
    archiveClassifier.set("")
    
    from(sourceSets.main.get().output)
    
    // Include ALL classes including stubs for dev testing
    manifest {
        attributes(
            "Implementation-Title" to "Rubidium Framework (Dev)",
            "Implementation-Version" to version,
            "Main-Class" to "rubidium.test.RubidiumTestHarness",
            "Rubidium-Tier" to "PLUS",
            "Rubidium-Premium" to "true"
        )
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Task to run the test harness
tasks.register<JavaExec>("runTestHarness") {
    group = "verification"
    description = "Run Rubidium test harness to verify plugin initialization"
    
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("rubidium.test.RubidiumTestHarness")
    
    // JVM args for better output
    jvmArgs = listOf("-Xmx512m")
}

// Task to run the UI launcher
tasks.register<JavaExec>("runLauncher") {
    group = "application"
    description = "Launch Rubidium with visual UI (Settings, Minimap, Admin Panel)"
    
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("rubidium.RubidiumLauncher")
    
    jvmArgs = listOf("-Xmx512m")
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
                        id.set("dequackdealer")
                        name.set("DeQuackDealer")
                    }
                }
            }
        }
    }
}
