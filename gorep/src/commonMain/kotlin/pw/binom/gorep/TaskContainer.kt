package pw.binom.gorep

interface TaskContainer {
    val tasks: List<Task>
    fun getTaskByName(name: String): Task
    fun addTask(task: Task): Task
}