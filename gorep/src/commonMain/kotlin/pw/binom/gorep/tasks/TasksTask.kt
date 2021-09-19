package pw.binom.gorep.tasks

import pw.binom.gorep.Context
import pw.binom.gorep.Task

class TasksTask(val context: Context) : Task {
    override val name: String
        get() = "tasks"

    override val description: String?
        get() = "Prints Tasks list"

    override fun getDependencies(): List<Task> = emptyList()

    override suspend fun run() {
        println("Tasks:")
        val maxNameSize = context.tasks.maxOf {
            it.name.length
        }?:0

        context.tasks.forEach {
            print(" ")
            print(it.name)
            if (it.description!=null){
                repeat((maxNameSize-it.name.length) + 2) {
                    print(" ")
                }
                print(it.description)
            }
            println()
        }
        println()
    }
}