package com.yellowtale.rubidium.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class DeployPluginTask : DefaultTask() {
    
    @get:Internal
    lateinit var extension: RubidiumPluginExtension
    
    @TaskAction
    fun deploy() {
        val serverDir = File(project.projectDir, extension.serverDir)
        val pluginsDir = File(serverDir, "plugins")
        
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
            logger.lifecycle("Created plugins directory: ${pluginsDir.absolutePath}")
        }
        
        val jarFile = project.tasks.getByName("jar").outputs.files.singleFile
        val targetFile = File(pluginsDir, "${extension.pluginId}.jar")
        
        jarFile.copyTo(targetFile, overwrite = true)
        logger.lifecycle("Deployed plugin to: ${targetFile.absolutePath}")
        
        val hotReloadFile = File(pluginsDir, ".hotreload")
        hotReloadFile.writeText(extension.pluginId)
        logger.lifecycle("Triggered hot-reload for: ${extension.pluginId}")
    }
}
