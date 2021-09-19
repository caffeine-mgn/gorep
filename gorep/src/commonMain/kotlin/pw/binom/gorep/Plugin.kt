package pw.binom.gorep

interface Plugin {
    fun apply(project: Project, context: Context)
}