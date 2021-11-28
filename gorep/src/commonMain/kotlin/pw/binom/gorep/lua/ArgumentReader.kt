package pw.binom.gorep.lua

import pw.binom.lua.InputVarargs
import pw.binom.lua.LuaValue
import pw.binom.lua.checkedTable
import pw.binom.lua.isTable

interface ArgumentReader {
    fun get(name: String, index: Int): LuaValue

    companion object {
        fun create(input: InputVarargs) =
            when {
                input.size == 0 -> NoArgArgumentReader
                input.size == 1 && input[0].isTable -> NamedArgumentReader(input)
                else -> IndexedArgumentReader(input)
            }
    }
}

class NamedArgumentReader(input: InputVarargs) : ArgumentReader {
    init {
        require(input.size == 1)
    }

    private val table = input[0].checkedTable()
    override fun get(name: String, index: Int): LuaValue = table[LuaValue.of(name)]
}

class IndexedArgumentReader(val input: InputVarargs) : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = input[index]
}

object NoArgArgumentReader : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = LuaValue.Nil
}