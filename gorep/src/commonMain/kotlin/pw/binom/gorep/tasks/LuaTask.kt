package pw.binom.gorep.tasks

import pw.binom.gorep.lua.LuaContainer
import pw.binom.gorep.Task
import pw.binom.lua.LuaEngine
import pw.binom.lua.LuaValue
import kotlin.jvm.JvmName

class LuaTask2(
    val engine: LuaEngine,
    override val name: String,
    override val clazz: String,
    var executeFunc: LuaValue?,
) : AbstractTask() {
    override suspend fun execute() {
        if (executeFunc != null) {
            engine.call(executeFunc!!)
        }
    }
}