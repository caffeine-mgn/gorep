package pw.binom.gorep

import pw.binom.io.file.File
import pw.binom.lua.LuaValue

interface Task {
    var description: String?
    val name: String
    var taskSelector: TaskSelector
    val inputFiles: MutableList<File>
    val outputFiles: MutableList<File>
    val clazz: String
    var enabled: Boolean
    var runOnSource: Boolean
    suspend fun run()
    fun doFirst(func: suspend () -> Unit)
    fun doLast(func: suspend () -> Unit)
}