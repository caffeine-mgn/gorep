package pw.binom.gorep

class LinkedTask(override val name: String) : Task {
    var implement: Task? = null
        set(value) {
            if (value == null && field != null) {
                if (overrideDescription == null) {
                    overrideDescription = field!!.description
                }
                if (overrideSelector == null) {
                    overrideSelector = field!!.taskSelector
                }
            }
            if (value != null) {
                if (overrideDescription != null) {
                    value.description = overrideDescription
                    overrideDescription = null
                }
                if (overrideSelector != null) {
                    value.taskSelector = overrideSelector!!
                    overrideSelector = null
                }
            }
            field = value
        }

    private var overrideDescription: String? = null

    override var description: String?
        get() {
            if (implement != null) {
                return implement!!.description
            }
            return overrideDescription
        }
        set(value) {
            if (implement != null) {
                implement!!.description = value
            } else {
                overrideDescription = value
            }
        }

    private var overrideSelector: TaskSelector? = null
    override var taskSelector: TaskSelector
        get() {
            if (implement != null) {
                return implement!!.taskSelector
            }
            return overrideSelector ?: TaskSelector.NoTasks
        }
        set(value) {
            if (implement != null) {
                implement!!.taskSelector = value
            } else {
                overrideSelector = value
            }
        }
    override val clazz: String
        get() = implement?.clazz ?: "linkable_task"

    override suspend fun run() {
        val task = implement ?: TODO("Task not resolverd")
        task.run()
    }
}