package pw.binom.gorep.tasks

import pw.binom.gorep.Task

abstract class AbstractTask : Task {
    protected abstract fun resolveTasksDependencies(): List<Task>
    private var dependencies: List<Task>? = null

    override fun getDependencies(): List<Task> {
        if (dependencies == null) {
            dependencies = resolveTasksDependencies()
        }
        return dependencies!!
    }
}