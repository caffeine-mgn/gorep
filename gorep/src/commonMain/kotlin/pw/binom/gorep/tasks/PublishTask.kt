package pw.binom.gorep.tasks

import pw.binom.ByteBuffer
import pw.binom.DEFAULT_BUFFER_SIZE
import pw.binom.asyncInput
import pw.binom.crypto.Sha256MessageDigest
import pw.binom.gorep.*
import pw.binom.gorep.repository.Repository
import pw.binom.io.file.File
import pw.binom.io.file.extension
import pw.binom.io.file.openRead
import pw.binom.io.file.relative
import pw.binom.io.use
import pw.binom.nextUuid
import kotlin.random.Random

class PublishTask(val context: Context, val project: Project, val repository: Repository) : Task {
    override val name: String = "publish_${repository.name}"

    override val description: String?
        get() = "Publishing to ${repository.name} repository"

    private fun findBuildTask() = context.tasks.asSequence().mapNotNull {
        if (it !is BuildTask) {
            return@mapNotNull null
        }
        if (it.project != project) {
            return@mapNotNull null
        }
        it
    }.firstOrNull()

    override fun getDependencies(): List<Task> {
        val buildTask = findBuildTask() ?: TODO("Can't find build task")
        return listOf(buildTask)
    }

    override suspend fun run() {
        val buildTask = findBuildTask() ?: TODO()
        if (!buildTask.targetArchive.isFile) {
            TODO("${buildTask.targetArchive} not exist")
        }
        val shaHex = buildTask.targetArchive.calcSha256().toHex()
        val files = HashMap<File, String>()
        val includeMeta = project.info.metas.mapNotNull {
            if (it.file == null || it.file.isBlank()) {
                return@mapNotNull it
            }
            val f = project.root.relative(it.file)
            if (!f.isFile) {
                return@mapNotNull null
            }
            val newFileName = Random.nextUuid().toShortString() + f.extension.let { if (it == null) "" else ".$it" }
            files[f] = newFileName
            it.copy(file = newFileName)
        }
        val meta = ArtifactMetaInfo(
            name = project.info.name,
            title = project.info.title,
            version = project.info.version,
            sha256 = shaHex,
            dependencies = project.dependencies
                .filter { it.type == DepType.EXTERNAL }
                .map { Dependency(name = it.name, version = it.version) },
            metas = includeMeta,
        )
        repository.publish(
            meta = meta,
            archive = buildTask.targetArchive
        )
        files.forEach { (file, name) ->
            file.openRead().use { fileStream ->
                repository.publishMeta(
                    name = meta.name,
                    version = meta.version,
                    fileName = name,
                    input = fileStream.asyncInput(),
                )
            }
        }
    }
}

fun File.calcSha256(bufferSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val sha = Sha256MessageDigest()
    ByteBuffer.alloc(bufferSize).use { buf ->
        openRead().use {
            while (true) {
                buf.clear()
                val l = it.read(buf)
                if (l == 0) {
                    break
                }
                buf.flip()
                sha.update(buf)
            }
        }
    }
    return sha.finish()
}