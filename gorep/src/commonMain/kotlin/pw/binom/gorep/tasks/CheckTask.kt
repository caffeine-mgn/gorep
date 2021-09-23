package pw.binom.gorep.tasks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pw.binom.gorep.*
import pw.binom.io.file.deleteRecursive
import pw.binom.io.file.name
import pw.binom.io.file.readText
import pw.binom.io.file.relative
import pw.binom.logger.Logger
import pw.binom.logger.info
import pw.binom.logger.warn


class CheckTask(val context: Context, val project: Project, val force: Boolean, override val name: String) : Task {

    private val logger = Logger.getLogger(name)

    override fun getDependencies(): List<Task> = emptyList()

    private suspend fun install(dep: Dep) {
        context.repositoryService.localCache.install(
            destination = project.addonsDir,
            name = dep.name,
            version = dep.version,
        )
    }

    private suspend fun checkDep(dep: Dep) {
        val meta = project.addonsDir.relative("${dep.name}.dependency.json")
        val addon = project.addonsDir.relative(dep.name)
        if (!meta.isFile || !addon.isDirectory) {
            logger.info("Install ${dep.name}:${dep.version}")
            meta.deleteRecursive()
            addon.deleteRecursive()
            install(dep)
            return
        }
        val info = DepInfo.fromJsonText(meta.readText())
        if (info.version.compareTo(dep.version) != 0) {
            logger.info("Update ${dep.name}:${info.version}->${dep.version}")
            meta.deleteRecursive()
            addon.deleteRecursive()
            install(dep)
            return
        }
        val depInfo = context.repositoryService.localCache.find(name = dep.name, version = dep.version)
            ?: throw RuntimeException("Dependency ${dep.name}:${dep.version} lost")

        if (info.sha != depInfo.sha256) {
            logger.info("Reinstall ${dep.name}:${dep.version}")
            meta.deleteRecursive()
            addon.deleteRecursive()
            install(dep)
            return
        }
    }

    private suspend fun removeDeprecatedDependencies(actualDependencies: List<String>) {
        val files = project.addonsDir.list()
//        files.forEach {
//            if (it.isDirectory && it.name !in actualDependencies && it.name != project.name) {
//                it.deleteRecursive()
//            }
//        }
        files.forEach {
            if (it.isFile && it.name.endsWith(".dependency.json")) {
                val depName = it.name.removeSuffix(".dependency.json")
                if (depName !in actualDependencies) {
                    logger.info("Removing Deprecated $depName addon")
                    it.delete()
                    project.addonsDir.relative(depName).deleteRecursive()
                }
            }
            if (it.isDirectory && it.name != project.name && it.name !in actualDependencies) {
                logger.warn("Found unmanaged addon ${it.name}")
            }
        }
    }

    override suspend fun run() {
        project.resolveDependencies(context.repositoryService, forceUpdate = force)
        project.dependencies.forEach {
            checkDep(it)
        }
        removeDeprecatedDependencies(project.dependencies.map { it.name })
    }
}

@Serializable
class DepInfo(val sha: String, val version: Version) {
    companion object {
        fun fromJsonText(text: String) = Json.decodeFromString(serializer(), text)
    }

    fun toJsonText() = Json.encodeToString(serializer(), this)
}