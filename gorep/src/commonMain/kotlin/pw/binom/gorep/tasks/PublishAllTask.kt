package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Task

class PublishAllTask(val context: Context) : Task {
    override val name: String
        get() = "publish"

    override val description: String
        get() = "Publishing to all repositories"

    override fun getDependencies(): List<Task> =
        context.tasks.filter { it is PublishTask }

    override suspend fun run() {

    }
}