package pw.binom.gorep

import pw.binom.io.file.File

interface Task {
    var description: String?
    val name: String
    var taskSelector: TaskSelector
    val inputFiles: MutableList<File>
    val outputFiles: MutableList<File>
    val clazz: String
    var enabled: Boolean
    var runOnSource: Boolean
    fun define(context: Context) {}
    suspend fun run()
}