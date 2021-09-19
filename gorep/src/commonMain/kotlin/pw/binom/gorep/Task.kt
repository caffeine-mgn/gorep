package pw.binom.gorep

interface Task {
    val description: String?
        get() = null
    val name: String
    fun getDependencies(): List<Task>
    fun define(context: Context) {}
    suspend fun run()
}