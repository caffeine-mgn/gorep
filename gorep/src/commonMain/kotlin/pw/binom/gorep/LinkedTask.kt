package pw.binom.gorep

import pw.binom.io.file.File

class LinkedTask(override val name: String) : Task {
    var implement: Task? = null
        private set

    fun resolve(task: Task) {
        if (implement != null) {
            throw IllegalStateException("Linked task already resolved")
        }
        implement = task
        if (overrideDescription != null) {
            task.description = overrideDescription
            overrideDescription = null
        }
        if (overrideSelector != null) {
            task.taskSelector = overrideSelector!!
            overrideSelector = null
        }
        if (overrideRunOnSource != null) {
            task.runOnSource = overrideRunOnSource!!
        }
        if (overrideInputFiles.isNotEmpty()) {
            task.inputFiles += overrideInputFiles
            overrideInputFiles.clear()
        }
        if (overrideOutputFiles.isNotEmpty()) {
            task.outputFiles += overrideOutputFiles
            overrideOutputFiles.clear()
        }
    }

    private var overrideDescription: String? = null

    override var description: String?
        get() = implement?.description ?: overrideDescription
        set(value) {
            if (implement == null) {
                overrideDescription = value
            } else {
                implement!!.description = value
            }
        }

    private var overrideSelector: TaskSelector? = null
    override var taskSelector: TaskSelector
        get() = implement?.taskSelector ?: overrideSelector ?: TaskSelector.NoTasks
        set(value) {
            if (implement == null) {
                overrideSelector = value
            } else {
                implement!!.taskSelector = value
            }
        }
    private var overrideInputFiles = ArrayList<File>()
    override val inputFiles: MutableList<File>
        get() = implement?.inputFiles ?: overrideInputFiles
    private var overrideOutputFiles = ArrayList<File>()
    override val outputFiles: MutableList<File>
        get() = implement?.outputFiles ?: overrideOutputFiles
    private var overrideEnabled: Boolean = true
    override var enabled: Boolean
        get() = implement?.enabled ?: overrideEnabled
        set(value) {
            if (implement == null) {
                overrideEnabled = value
            } else {
                implement!!.enabled = value
            }
        }

    private var overrideRunOnSource: Boolean? = null

    override var runOnSource: Boolean
        get() = implement?.runOnSource ?: overrideRunOnSource ?: false
        set(value) {
            if (implement == null) {
                overrideRunOnSource = value
            } else {
                implement!!.runOnSource = value
            }
        }
    override val clazz: String
        get() = implement?.clazz ?: "linkable_task"


    override suspend fun run() {
        val task = implement ?: throw IllegalStateException("Task not resolved")
        task.run()
    }
}