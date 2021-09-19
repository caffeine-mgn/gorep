package pw.binom.gorep

import pw.binom.Environment
import pw.binom.getEnv
import pw.binom.logger.Logger
import pw.binom.logger.severe

class StandardVariableReplacer(val properties: Map<String, String>) : VariableReplacer {
    private val logger = Logger.getLogger("StandardVariableReplacer")
    private suspend fun searchValue(value: String): String? {
        if (!value.startsWith("{") || !value.endsWith("}")) {
            return null
        }
        val variableName = value.removePrefix("{").removeSuffix("}")
        if (variableName.startsWith("env.")) {
            val env = Environment.getEnv(variableName.removePrefix("env."))
            if (env == null) {
                logger.severe("Can't find Environment Variable \"$variableName\"")
            }
            return env
        }
        val prop = properties[variableName]
        if (prop == null) {
            logger.severe("Can't find property \"$variableName\"")
        }
        return prop
    }

    override suspend fun replace(value: String): String? = searchValue(value)
}