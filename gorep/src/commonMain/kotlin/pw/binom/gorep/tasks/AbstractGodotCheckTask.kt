package pw.binom.gorep.tasks

import pw.binom.gorep.*
import pw.binom.io.file.deleteRecursive

abstract class AbstractGodotCheckTask(val project: Project) : AbstractTask() {

    protected abstract fun check(name: String, version: Version)

    protected fun check(name: String, version: Version, updateCache: Boolean) {
//        val actualSha = context.repositoryService.getSha256(name, version = version, updateCache = updateCache)
//        val currentSha = project.getAddonSha(name)
//        val currentVersion = project.getAddonVersion(name)
//
//        if (currentSha == null || currentVersion == null || actualSha != currentSha || currentVersion.compareTo(version)!=0) {
//            update(force = true, name = name, version = version)
//            return
//        }
    }

    protected fun update(force: Boolean, name: String, version: Version) {
//        project.getAddonDirection(name)?.deleteRecursive()
//        context.repositoryService.store(
//            projectRoot = project.root,
//            name = name,
//            version = version,
//            updateCache = !force
//        )
    }

    override suspend fun execute() {
//        project.info.dependencies.forEach { dep ->
//            if (dep.name != null && dep.version != null) {
//                check(name = dep.name, version = dep.version)
//            }
//        }
    }
}