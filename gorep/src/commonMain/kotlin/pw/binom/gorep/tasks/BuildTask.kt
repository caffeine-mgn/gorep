package pw.binom.gorep.tasks

import pw.binom.compression.tar.TarEntityType
import pw.binom.compression.tar.TarWriter
import pw.binom.compression.zlib.GZIPOutput
import pw.binom.copyTo
import pw.binom.date.Date
import pw.binom.gorep.*
import pw.binom.io.file.*
import pw.binom.io.use
import pw.binom.logger.Logger

class BuildTask(val context: Context, val project: Project) : Task {

    override val description: String?
        get() = "Builds tar.gz file with addon"

    private fun findConfigTask() = context.tasks.asSequence().mapNotNull {
        if (it !is ConfigTask) {
            return@mapNotNull null
        }
        if (it.project != project) {
            return@mapNotNull null
        }
        it
    }.firstOrNull()

    private val logger = Logger.getLogger("BuildTask")
    val targetArchive = project.makeBuildDir().relative(ARCHIVE_FILE)

    //    val targetArchiveTar = project.makeBuildDir().relative("target.tar")
    override val name: String
        get() = "build"

    override fun getDependencies(): List<Task> = listOf(findConfigTask() ?: TODO())

    private fun filter(file: File): Boolean {
        if (project.info.excludes.isNotEmpty()) {
            val internalName = getRelativeArchivePath(root = project.root, file = file)
            if (internalName in project.info.excludes) {
                return false
            }
        }
        if (file.isFile && file.extension.lowercase() == "import") {
            return false
        }
        if (file == project.root.relative(".mono")) {
            return false
        }
        if (file == project.root.relative(".import")) {
            return false
        }
        if (file == project.buildDir) {
            return false
        }
        if (file == project.projectFile) {
            return false
        }
        if (file.parent == project.root && file.name == GODOT_PROJECT_FILE) {
            return false
        }
        if (file.parent == project.addonsDir && file.name != project.info.name) {
            return false
        }
        return true
    }

    override suspend fun run() {
        val startupScriptFile = project.pluginDir.relative(project.info.script)
        if (!startupScriptFile.isFile) {
            TODO("Can't find start plugin file \"$startupScriptFile\"")
        }
        targetArchive.parent!!.mkdirs()
        targetArchive.delete()
        val addonRoot = project.addonsDir.relative(project.name)
        if (!addonRoot.isDirectory){
            throw FileNotFoundException("Addon dir \"$addonRoot\" not found")
        }
        TarWriter(GZIPOutput(targetArchive.parent!!.relative(targetArchive.name).openWrite(), level = 9)).use { tar ->
            addonRoot.list().forEach {
                if (it.isDirectory) {
                    packDir(
                        root = addonRoot,
                        file = it,
                        filter = this::filter,
                        tarWriter = tar,
                    )
                }
                if (it.isFile && filter(it)) {
                    packFile(
                        root = addonRoot,
                        file = it,
                        tarWriter = tar
                    )
                }
            }
        }
    }
}

private fun getRelativeArchivePath(root: File, file: File): String {
    val name = file.toString().substring(root.toString().length + 1).replace('\\', '/')
    return if (file.isDirectory) {
        "$name/"
    } else {
        name
    }
}

private fun packDir(root: File, file: File, tarWriter: TarWriter, filter: (File) -> Boolean) {
    if (!filter(file)) {
        return
    }
    tarWriter.newEntity(
        name = getRelativeArchivePath(root = root, file = file),
        mode = "100777".toUShort(8),
        uid = 0.toUShort(),
        gid = 0.toUShort(),
        time = Date.nowTime,
        type = TarEntityType.DIRECTORY
    ).close()
    file.iterator().forEach {
        if (it.isFile && filter(it)) {
            packFile(root = root, file = it, tarWriter = tarWriter)
        }
        if (it.isDirectory) {
            packDir(root = root, file = it, tarWriter = tarWriter, filter = filter)
        }
    }
}

private fun packFile(root: File, file: File, tarWriter: TarWriter) {
    tarWriter.newEntity(
        name = getRelativeArchivePath(root = root, file = file),
        mode = "100777".toUShort(8),
        uid = 0.toUShort(),
        gid = 0.toUShort(),
        time = Date.nowTime,
        type = TarEntityType.NORMAL
    ).use { tar ->
        file.openRead().use { fileData ->
            fileData.copyTo(tar)
        }
    }
}