package pw.binom.gorep

interface Context {
    val properties: Map<String, String>
    val verbose: Boolean
    val status: Status
    val localCache:LocalCache

    enum class Status {
        NEW,
        TASKS_RESOLVE,
        FINISH
    }
}