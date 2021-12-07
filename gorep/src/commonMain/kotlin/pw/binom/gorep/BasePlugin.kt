package pw.binom.gorep

import pw.binom.gorep.tasks.DependenciesTree
import pw.binom.gorep.tasks.TasksTask

object BasePlugin : Plugin {
    override suspend fun apply(project: Project) {
        project.addTask(TasksTask(project = project))
        project.addTask(DependenciesTree(project = project))
    }
}