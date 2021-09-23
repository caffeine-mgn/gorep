package pw.binom.gorep

class TaskController(val context: Context) {

    private fun checkCycleDependency(task: Task) {
        val checked = HashSet<Task>()
        val forCheck = ArrayList<Task>()

        fun check(t: Task) {
            if (t == task) {
                throw RuntimeException("Cycle dependency between tasks ${task.name} and ${t.name}")
            }
            checked += t
            t.getDependencies().forEach {
                if (it in checked) {
                    return@forEach
                }
                forCheck.add(it)
            }
        }
        task.getDependencies().forEach {
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
        val dependencies = task.getDependencies()
        dependencies.forEach {
            run(it)
        }
        println("> Task ${task.name}")
        task.run()
    }

    suspend fun run(tasks: List<Task>) {
        tasks.forEach {
            checkCycleDependency(it)
        }
        tasks.forEach {
            run(it)
        }
    }
}