package pw.binom.gorep

interface Plugin {
    suspend fun apply(project: Project)
}