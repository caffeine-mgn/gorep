package pw.binom.gorep

interface VariableReplacer {
    suspend fun replace(value: String): String?
}