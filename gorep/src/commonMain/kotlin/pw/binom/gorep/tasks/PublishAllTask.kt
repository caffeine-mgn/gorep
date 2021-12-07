package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Task

class PublishAllTask() : AbstractTask() {
    override val name: String
        get() = "publish"
    override val clazz: String
        get() = "meta"

    override var description: String? = "Publishing to all repositories"

    override suspend fun execute() {

    }
}