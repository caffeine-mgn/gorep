package pw.binom.gorep

class TaskRunner(private val dependencies: HashMap<Task, List<Task>>) {
    private val alreadyExecuted = HashSet<Task>()
    suspend fun run(tasks: List<Task>) {
        val unwrapedTask = tasks.map { unwrap(it) }
        unwrapedTask.forEach {
            checkCycleDependency(it)
        }
        unwrapedTask.forEach {
            run(it)
        }
    }

    private fun checkCycleDependency(task: Task) {
        val checked = HashSet<Task>()
        val forCheck = ArrayList<Task>()

        fun check(t: Task) {
            if (t == task) {
                throw RuntimeException("Cycle dependency between tasks ${task.name} and ${t.name}")
            }
            checked += t
            dependencies[t]?.forEach {
                if (it in checked) {
                    return@forEach
                }
                forCheck.add(it)
            }
        }
        dependencies[task]?.forEach {
            forCheck += it
        }
        while (forCheck.isNotEmpty()) {
            val e = forCheck.removeLast()
            check(e)
        }
    }

    private suspend fun run(task: Task) {
        if (task in alreadyExecuted) {
            return
        }
        alreadyExecuted += task
        val dependencies = dependencies[task] ?: emptyList()
        dependencies.forEach {
            run(it)
        }
        println("> Task ${task.name}")
        task.run()
    }
}

private fun unwrap(task: Task) =
    if (task is LinkedTask) {
        if (task.implement == null) {
            TODO("Can't resolve task \"${task.name}\"")
        }
        task.implement!!
    } else {
        task
    }

object TaskController {
    fun resolve(project: Project): TaskRunner {
        val dependencies = HashMap<Task, List<Task>>()
        project.tasks.forEach {
            val deps = it.taskSelector.select(project).map { unwrap(it) }
            dependencies[unwrap(it)] = deps
        }
        return TaskRunner(dependencies)
    }
}