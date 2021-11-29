package pw.binom.gorep.lua

import pw.binom.gorep.Context
import pw.binom.gorep.Plugin
import pw.binom.gorep.Project
import pw.binom.io.file.parent
import pw.binom.io.file.readText
import pw.binom.io.file.relative

object LuaPlugin : Plugin {
    override fun apply(project: Project, context: Context) {
        val lua = project.info.lua ?: return
        context.addTask(GenerateLuaMetaTask(context = context, project = project))
        var workDirectory = project.pluginDir

        val scriptFile = project.pluginDir.relative(lua)
        workDirectory = scriptFile.parent ?: workDirectory
        if (!scriptFile.isFile) {
            TODO("Lua file not found")
        }
        val scriptText = scriptFile.readText()

        val e = LuaContainer(project, context)
        e.execute(script = scriptText, workDirectory = workDirectory, isMain = true)
    }
}