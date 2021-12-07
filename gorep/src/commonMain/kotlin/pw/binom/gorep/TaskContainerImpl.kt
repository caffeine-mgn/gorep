package pw.binom.gorep

class TaskContainerImpl: TaskContainer {
    private val _tasks = ArrayList<Task>()
    override fun getTaskByName(name: String): Task {
        val found = _tasks.find { it.name == name }
        if (found != null) {
            return found
        }
        val linkedTask = LinkedTask(name = name)
        _tasks += linkedTask
        return linkedTask
    }

    override val tasks: List<Task>
        get() = _tasks

    override fun addTask(task: Task): Task {
        if (task is LinkedTask) {
            throw IllegalStateException("Can't add linked task via external api")
        }
        val linkedTask = _tasks.find { it.name == task.name }
        if (linkedTask != null) {
            if (linkedTask is LinkedTask) {
                linkedTask.resolve(task)
                return linkedTask
            }
            throw IllegalArgumentException("Task \"${task.name}\" already exist")
        }
        _tasks += task
        return task
    }
}