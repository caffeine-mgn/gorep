package pw.binom.gorep

interface Context {
    val tasks: List<Task>
    val repositoryService: RepositoryService
    val variableReplacer: VariableReplacer
    fun addTask(task: Task)
}