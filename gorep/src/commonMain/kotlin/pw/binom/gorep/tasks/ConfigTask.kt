package pw.binom.gorep.tasks

import pw.binom.Output
import pw.binom.gorep.ADDONS_DIR
import pw.binom.gorep.Project
import pw.binom.gorep.Task
import pw.binom.io.bufferedWriter
import pw.binom.io.file.*
import pw.binom.io.use

class ConfigTask(val project: Project) : AbstractTask() {
    companion object {
        val BASE_NAME = "config"
    }

    override val name: String
        get() = BASE_NAME
    override val clazz: String
        get() = "config"
    val pluginCfgDir = project.pluginDir.relative("plugin.cfg")
    override var description: String? = "Generates res://$ADDONS_DIR/${project.pluginDir.name}/plugin.cfg file"

//    override fun getDependencies(): List<Task> = emptyList()

    override suspend fun run() {

        pluginCfgDir.parent!!.mkdirs()
        pluginCfgDir.openWrite().use {
            makePluginCfg(it)
        }
        val projectFile = project.root.relative("project.godot")
        if (!projectFile.isFile && !projectFile.isExist) {
            projectFile.openWrite().close()
        }
    }

    private fun makePluginCfg(output: Output) {
        output.bufferedWriter().let {
            it.append("[plugin]\n")
                .append("name=\"${project.info.title}\"\n")
                .append("version=\"${project.info.version}\"\n")
                .append("script=\"${project.info.script}\"\n")
                .append("description=\"${project.info.description ?: ""}\"\n")
                .append("author=\"${project.info.author ?: ""}\"\n")
            it.flush()
        }
    }
}