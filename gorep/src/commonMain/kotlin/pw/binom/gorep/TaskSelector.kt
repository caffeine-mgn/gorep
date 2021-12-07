package pw.binom.gorep

import kotlin.reflect.KClass

sealed interface TaskSelector {
    fun select(project: Project): List<Task>
    operator fun plus(task: Task): TaskSelector =
        this + SelectedTask(task)

    operator fun plus(selector: TaskSelector): TaskSelector {
        if (selector is Composer) {
            selector.list += this
            return selector
        }
        val composer = Composer()
        composer.list += this
        composer.list += selector
        return composer
    }

    object NoTasks : TaskSelector {
        override fun select(project: Project): List<Task> = emptyList()
        override fun plus(selector: TaskSelector): TaskSelector = selector
    }

    class WithName(val name: String, val optional: Boolean) : TaskSelector {
        override fun select(project: Project): List<Task> {
            val f = project.tasks.find { it.name == name }
            if (f == null && optional) {
                return emptyList()
            }
            if (f == null) {
                TODO("Task \"$name\" not found")
            }
            return listOf(f)
        }
    }

    class SelectedTask(val task: Task) : TaskSelector {
        override fun select(project: Project): List<Task> =
            listOf(task)
    }

    class CustomSelector(val selector: (Task) -> Boolean) : TaskSelector {
        override fun select(project: Project): List<Task> = project.tasks.filter(selector)
    }
    class WithType(val type:KClass<out Task>) : TaskSelector {
        override fun select(project: Project): List<Task> = project.tasks.filter{
            if (type.isInstance(it)) {
                return@filter true
            }
            if (it is LinkedTask && it.implement!=null){
                type.isInstance(it.implement)
            }
            false
        }
    }

    class Composer : TaskSelector {
        val list = ArrayList<TaskSelector>()
        override fun select(project: Project): List<Task> =
            list.flatMap { it.select(project) }

        override fun plus(selector: TaskSelector): TaskSelector {
            if (selector is Composer) {
                list += selector.list
                return this
            }
            list += selector
            return this
        }
    }
}