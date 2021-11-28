package pw.binom.gorep.tasks

import pw.binom.gorep.lua.LuaContainer
import pw.binom.gorep.Task
import kotlin.jvm.JvmName

class LuaTask(
    val luaContainer: LuaContainer,
    val functionName: String,
    override val name: String,
    override val clazz: String,
) : AbstractTask() {
    override suspend fun run() {
        luaContainer.call(functionName)
    }
}