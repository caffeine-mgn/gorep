package pw.binom.gorep

class TaskController(val context: Context) {

    private val dependencies = HashMap<Task, List<Task>>()

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

    val alreadyExecuted = HashSet<Task>()

    private suspend fun run(task: Task) {
        if (task in alreadyExecuted) {
            return
        }
        alreadyExecuted += task
        val dependencies = dependencies[task]?: emptyList()
        dependencies.forEach {
            run(it)
        }
        println("> Task ${task.name}")
        task.run()
    }

    fun resolve(project: Project) {
        context.tasks.forEach {
            val deps = it.taskSelector.select(context, project).map { unwrap(it) }
            dependencies[unwrap(it)] = deps
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

    suspend fun run(tasks: List<Task>) {
        val unwrapedTask = tasks.map { unwrap(it) }
        unwrapedTask.forEach {
            checkCycleDependency(it)
        }
        unwrapedTask.forEach {
            run(it)
        }
    }
}