package pw.binom.gorep

import pw.binom.gorep.tasks.*

object GodotPlugin : Plugin {
    override fun apply(project: Project, context: Context) {
        context.addTask(BuildTask(project = project, context = context))
        context.addTask(ConfigTask(project = project))
        context.addTask(CheckTask(project = project, context = context))
        context.addTask(UpdateTask(project = project, context = context))
        var publishTaskExist = false
        context.repositoryService.repositories.forEach { repo ->
            if (project.info.repositories.find { it.name == repo.name }?.publish != true) {
                return@forEach
            }
            publishTaskExist = true
            context.addTask(PublishTask(context = context, project = project, repository = repo))
        }
        if (publishTaskExist) {
            context.addTask(PublishAllTask(context))
        }
    }
}