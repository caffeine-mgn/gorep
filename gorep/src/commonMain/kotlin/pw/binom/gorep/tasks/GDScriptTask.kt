package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Task

class GDScriptTask(
    val context: Context,
    private val dependencies: List<String>,
    override val name: String
) : AbstractTask() {
    override fun resolveTasksDependencies(): List<Task> =
        dependencies.map { taskName ->
            context.tasks.find { it.name == taskName } ?: throw RuntimeException("Can't find task $taskName")
        }

    override suspend fun run() {
        TODO("Not yet implemented")
    }
}