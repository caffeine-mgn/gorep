package pw.binom.gorep

interface Context {
    val tasks: List<Task>
    val repositoryService: RepositoryService
    fun addTask(task: Task)
}