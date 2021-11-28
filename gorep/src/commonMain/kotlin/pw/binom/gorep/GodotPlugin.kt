package pw.binom.gorep

import pw.binom.gorep.tasks.*

object GodotPlugin : Plugin {
    override fun apply(project: Project, context: Context) {
        context.addTask(BuildTask(project = project, context = context)).also {
            it.taskSelector += it.taskSelector + context.getTaskByName(ConfigTask.BASE_NAME)
        }
        context.addTask(ConfigTask(project = project))
        context.addTask(CheckTask(project = project, context = context, name = "check", force = false))
        context.addTask(CheckTask(project = project, context = context, name = "check_update", force = true))
        context.addTask(UpdateTask(project = project, context = context))
        var publishTaskExist = false
        context.repositoryService.repositories.forEach { repo ->
            if (project.info.repositories.find { it.name == repo.name }?.publish != true) {
                return@forEach
            }
            publishTaskExist = true
            context.addTask(PublishTask(context = context, project = project, repository = repo)).also {
                it.taskSelector = it.taskSelector + TaskSelector.WithType(BuildTask::class)
            }
        }
        if (publishTaskExist) {
            context.addTask(PublishAllTask(context)).also {
                it.taskSelector = it.taskSelector + TaskSelector.WithType(PublishTask::class)
            }
        }
    }
}