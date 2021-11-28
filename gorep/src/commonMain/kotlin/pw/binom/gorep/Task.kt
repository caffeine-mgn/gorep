package pw.binom.gorep

interface Task {
    var description: String?
    val name: String
    var taskSelector: TaskSelector
    val clazz: String
    fun define(context: Context) {}
    suspend fun run()
}