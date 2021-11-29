package pw.binom.gorep.tasks

import pw.binom.gorep.Task
import pw.binom.gorep.TaskSelector
import pw.binom.io.file.File

abstract class AbstractTask : Task {
    override var description: String? = null
    override var taskSelector: TaskSelector = TaskSelector.NoTasks
    override val inputFiles = ArrayList<File>()
    override val outputFiles = ArrayList<File>()
    override var enabled: Boolean = true
    override var runOnSource: Boolean = false
}