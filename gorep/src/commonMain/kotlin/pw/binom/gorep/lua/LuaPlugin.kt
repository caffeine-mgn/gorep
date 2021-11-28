package pw.binom.gorep.lua

import pw.binom.gorep.Context
import pw.binom.gorep.Plugin
import pw.binom.gorep.Project
import pw.binom.io.file.parent
import pw.binom.io.file.readText
import pw.binom.io.file.relative

object LuaPlugin : Plugin {
    override fun apply(project: Project, context: Context) {
        context.addTask(GenerateLuaMetaTask(context = context, project = project))
        project.info.scripts.forEach { script ->
            var workDirectory = project.root
            val scriptText = if (script.file != null) {
                val scriptFile = project.root.relative(script.file)
                workDirectory = scriptFile.parent ?: workDirectory
                if (!scriptFile.isFile) {
                    TODO("Lua file not found")
                }
                scriptFile.readText()
            } else {
                script.script ?: ""
            }
            val e = LuaContainer(project, context)
            e.execute(script = scriptText, workDirectory = workDirectory, isMain = true)
        }
    }
}