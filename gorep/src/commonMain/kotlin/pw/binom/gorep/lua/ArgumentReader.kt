package pw.binom.gorep.lua

import pw.binom.lua.LuaValue
import pw.binom.lua.checkedTable
import pw.binom.lua.isTable
import pw.binom.lua.lua
import kotlin.jvm.JvmInline

interface ArgumentReader {
    fun get(name: String, index: Int): LuaValue

    companion object {
        fun create(input: List<LuaValue>, skipFirst: Boolean = false) =
            when {
                input.size == 0 -> NoArgArgumentReader
                input.size == 1 && input[0].isTable -> NamedArgumentReader(input[0].checkedTable().toValue())
                skipFirst && input.size == 2 && input[1].isTable -> NamedArgumentReader2(
                    input[1].checkedTable().toValue()
                )
                skipFirst -> IndexedArgumentReader2(input)
                else -> IndexedArgumentReader(input)
            }
    }
}

@JvmInline
value class NamedArgumentReader(val table: LuaValue.Table) : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = table[name.lua]
}

@JvmInline
value class NamedArgumentReader2(val table: LuaValue.Table) : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = table[name.lua]
}

class IndexedArgumentReader(val input: List<LuaValue>) : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = input[index]
}

class IndexedArgumentReader2(val input: List<LuaValue>) : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = input[index + 1]
}

object NoArgArgumentReader : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = LuaValue.Nil
}