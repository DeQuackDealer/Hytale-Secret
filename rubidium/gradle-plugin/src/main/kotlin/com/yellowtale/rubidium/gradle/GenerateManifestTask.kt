package com.yellowtale.rubidium.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateManifestTask : DefaultTask() {
    
    @get:Internal
    lateinit var extension: RubidiumPluginExtension
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        
        val content = buildString {
            appendLine("[plugin]")
            appendLine("id = \"${extension.pluginId}\"")
            appendLine("name = \"${extension.pluginName}\"")
            appendLine("version = \"${project.version}\"")
            appendLine("author = \"${extension.author}\"")
            appendLine("description = \"${extension.description}\"")
            appendLine("api_version = \"${extension.apiVersion}\"")
            appendLine()
            
            if (extension.dependencies.isNotEmpty()) {
                for (dep in extension.dependencies) {
                    appendLine("[[dependencies]]")
                    appendLine("id = \"$dep\"")
                    appendLine("required = true")
                    appendLine()
                }
            }
            
            if (extension.softDependencies.isNotEmpty()) {
                for (dep in extension.softDependencies) {
                    appendLine("[[dependencies]]")
                    appendLine("id = \"$dep\"")
                    appendLine("required = false")
                    appendLine()
                }
            }
        }
        
        file.writeText(content)
        logger.lifecycle("Generated plugin manifest: ${file.absolutePath}")
    }
}
