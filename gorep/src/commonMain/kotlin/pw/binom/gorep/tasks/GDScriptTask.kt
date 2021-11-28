package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Task

class GDScriptTask(
    val context: Context,
    override val name: String
) : AbstractTask() {
    override val clazz: String
        get() = "gdscript_task"

    override suspend fun run() {
        TODO("Not yet implemented")
    }
}