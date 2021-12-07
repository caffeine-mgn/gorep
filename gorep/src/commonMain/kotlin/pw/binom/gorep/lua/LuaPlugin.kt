package pw.binom.gorep.lua

import pw.binom.gorep.Context
import pw.binom.gorep.Plugin
import pw.binom.gorep.Project
import pw.binom.io.file.parent
import pw.binom.io.file.readText
import pw.binom.io.file.relative

object LuaPlugin : Plugin {
    override suspend fun apply(project: Project) {
        val lua = project.info.lua ?: return
        project.addTask(GenerateLuaMetaTask(project = project))
        var workDirectory = project.pluginDir

        val scriptFile = project.pluginDir.relative(lua)
        workDirectory = scriptFile.parent ?: workDirectory
        if (!scriptFile.isFile) {
            TODO("Lua file not found")
        }
        val scriptText = scriptFile.readText()
        project.luaContainer.execute(script = scriptText, workDirectory = workDirectory, isMain = true)
    }
}