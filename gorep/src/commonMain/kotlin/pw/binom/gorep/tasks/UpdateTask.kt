package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Project
import pw.binom.gorep.Task
import pw.binom.gorep.Version


class UpdateTask(context: Context, project: Project) : AbstractGodotCheckTask(context = context, project = project) {

    override fun check(name: String, version: Version) {
        check(name = name, version = version, updateCache = true)
    }

    override val name: String
        get() = "update"
    override val clazz: String
        get() = "update"
}