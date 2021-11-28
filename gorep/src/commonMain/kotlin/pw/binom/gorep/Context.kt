package pw.binom.gorep

interface Context {
    fun getTaskByName(name:String):Task
    val tasks: List<Task>
    val repositoryService: RepositoryService
    val variableReplacer: VariableReplacer
    val verbose: Boolean
    fun addTask(task: Task):Task
    fun findTasks(selector: TaskSelector):List<Task>
}