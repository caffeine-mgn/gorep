package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Task

class TasksTask(val context: Context) : AbstractTask() {
    override val name: String
        get() = "tasks"
    override val clazz: String
        get() = "info"

    override var description: String? = "Prints Tasks list"

    override suspend fun run() {
        println("Tasks:")
        val maxNameSize = context.tasks.maxOfOrNull {
            it.name.length
        } ?: 0

        context.tasks.forEach {
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