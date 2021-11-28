package pw.binom.gorep.tasks

import pw.binom.gorep.Task
import pw.binom.gorep.TaskSelector

abstract class AbstractTask : Task {
    override var description: String? = null
    override var taskSelector: TaskSelector = TaskSelector.NoTasks
}