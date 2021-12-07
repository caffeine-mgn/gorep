package pw.binom.gorep

import pw.binom.gorep.tasks.*

object GodotPlugin : Plugin {
    override suspend fun apply(project: Project) {
        project.addTask(BuildTask(project = project)).also {
            it.taskSelector += it.taskSelector + project.getTaskByName(ConfigTask.BASE_NAME)
        }
        project.addTask(ConfigTask(project = project))
        project.addTask(CheckTask(project = project, name = "check", force = false))
        project.addTask(CheckTask(project = project, name = "check_update", force = true))
        project.addTask(UpdateTask(project = project))
        var publishTaskExist = false
        project.repositories.forEach { repo ->
            if (project.info.repositories.find { it.name == repo.name }?.publish != true) {
                return@forEach
            }
            publishTaskExist = true
            project.addTask(PublishTask(project = project, repository = repo)).also {
                it.taskSelector = it.taskSelector + TaskSelector.WithType(BuildTask::class)
            }
        }
        if (publishTaskExist) {
            project.addTask(PublishAllTask()).also {
                it.taskSelector = it.taskSelector + TaskSelector.WithType(PublishTask::class)
            }
        }
    }
}