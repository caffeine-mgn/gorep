package pw.binom.gorep

import pw.binom.Input
import pw.binom.io.bufferedReader

class ConfigFile(val section: Map<String, Map<String, ConfigElement>>, val params: Map<String, ConfigElement>) {
    companion object {
        fun read(input: Input): ConfigFile {
            val reader = input.bufferedReader()
            val section = HashMap<String, HashMap<String, ConfigElement>>()
            val params = HashMap<String, ConfigElement>()
            var currentSection: HashMap<String, ConfigElement>? = null
            while (true) {
                val line = reader.readln() ?: break
                if (line.startsWith("[")) {
                    if (!line.endsWith("]")) {
                        TODO()
                    }
                    val sectionName = line.removePrefix("[").removeSuffix("]")
                    currentSection = section.getOrPut(sectionName) { HashMap() }
                } else {
                    val items = line.split('=', limit = 2)
                    if (items.size != 2) {
                        TODO()
                    }
                    val value = if (items[1].startsWith("\"") && items[1].endsWith("\"")) {
                        ConfigElement.CString(items[1].removePrefix("\"").removeSuffix("\""))
                    } else {
                        TODO()
                    }
                    if (currentSection != null) {
                        currentSection[items[0]] = value
                    } else {
                        params[items[0]] = value
                    }
                }
            }
            return ConfigFile(section = section, params = params)
        }
    }

    operator fun get(section: String, name: String) = this.section[section]?.get(name)
    operator fun get(name: String) = this.params[name]
}

sealed interface ConfigElement {
    class CString(val body: String) : ConfigElement
    class CMap(val body: Map<String, ConfigElement>) : ConfigElement
    class CArray(val body: Array<ConfigElement>)
}