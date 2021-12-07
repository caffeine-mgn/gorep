package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Project
import pw.binom.gorep.Task

class TasksTask(val project: Project) : AbstractTask() {
    override val name: String
        get() = "tasks"
    override val clazz: String
        get() = "info"

    override var description: String? = "Prints Tasks list"

    override suspend fun execute() {
        println("Tasks:")
        val maxNameSize = project.tasks.maxOfOrNull {
            it.name.length
        } ?: 0

        project.tasks.forEach {
            print(" ")
            print(it.name)
            if (it.description != null) {
                repeat((maxNameSize - it.name.length) + 2) {
                    print(" ")
                }
                print(it.description)
            }
            println()
        }
        println()
    }
}