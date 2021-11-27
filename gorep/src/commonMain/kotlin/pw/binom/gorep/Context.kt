package pw.binom.gorep

interface Context {
    val tasks: List<Task>
    val repositoryService: RepositoryService
    val variableReplacer: VariableReplacer
    val verbose: Boolean
    fun addTask(task: Task)
}