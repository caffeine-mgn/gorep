package pw.binom.gorep

import pw.binom.gorep.tasks.DependenciesTree
import pw.binom.gorep.tasks.TasksTask

object BasePlugin : Plugin {
    override fun apply(project: Project, context: Context) {
        context.addTask(TasksTask(context = context))
        context.addTask(DependenciesTree(context = context, project = project))
    }
}