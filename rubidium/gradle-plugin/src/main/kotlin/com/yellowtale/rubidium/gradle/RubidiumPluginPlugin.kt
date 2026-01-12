package com.yellowtale.rubidium.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.tasks.Jar as JarTask

class RubidiumPluginPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        
        val extension = project.extensions.create("rubidium", RubidiumPluginExtension::class.java)
        
        project.repositories.apply {
            mavenCentral()
            maven { 
                it.setUrl("https://repo.yellowtale.com/maven")
            }
        }
        
        project.dependencies.add("compileOnly", "com.yellowtale:rubidium-api:1.0.0")
        
        project.tasks.register("generatePluginManifest", GenerateManifestTask::class.java) { task ->
            task.group = "rubidium"
            task.description = "Generates the plugin.toml manifest file"
            task.extension = extension
            task.outputFile.set(project.layout.buildDirectory.file("resources/main/plugin.toml"))
        }
        
        project.tasks.named("processResources") { task ->
            task.dependsOn("generatePluginManifest")
        }
        
        project.tasks.named("jar", JarTask::class.java) { jar ->
            jar.archiveBaseName.set(extension.pluginId)
            jar.manifest.attributes(
                mapOf(
                    "Plugin-Id" to extension.pluginId,
                    "Plugin-Name" to extension.pluginName,
                    "Plugin-Version" to project.version.toString()
                )
            )
        }
        
        project.tasks.register("buildPlugin", Jar::class.java) { task ->
            task.group = "rubidium"
            task.description = "Builds the plugin JAR with all dependencies"
            task.dependsOn("jar")
            task.archiveClassifier.set("plugin")
            task.from(project.tasks.named("jar").map { (it as JarTask).archiveFile })
        }
        
        project.tasks.register("deployPlugin", DeployPluginTask::class.java) { task ->
            task.group = "rubidium"
            task.description = "Deploys the plugin to a running Rubidium server"
            task.extension = extension
            task.dependsOn("buildPlugin")
        }
    }
}

open class RubidiumPluginExtension {
    var pluginId: String = ""
    var pluginName: String = ""
    var author: String = ""
    var description: String = ""
    var apiVersion: String = "1.0.0"
    var dependencies: List<String> = emptyList()
    var softDependencies: List<String> = emptyList()
    var loadBefore: List<String> = emptyList()
    var serverDir: String = "run"
}
