package pw.binom.gorep.lua

import pw.binom.lua.*

interface ArgumentReader {
    fun get(name: String, index: Int): LuaValue

    companion object {
        fun create(input: List<LuaValue>) =
            when {
                input.size == 0 -> NoArgArgumentReader
                input.size == 1 && input[0].isTable -> NamedArgumentReader(input)
                else -> IndexedArgumentReader(input)
            }
    }
}

class NamedArgumentReader(input: List<LuaValue>) : ArgumentReader {
    init {
        require(input.size == 1)
    }

    private val table = input[0].checkedTable()
    override fun get(name: String, index: Int): LuaValue = table[name.lua]
}

class IndexedArgumentReader(val input: List<LuaValue>) : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = input[index]
}

object NoArgArgumentReader : ArgumentReader {
    override fun get(name: String, index: Int): LuaValue = LuaValue.Nil
}