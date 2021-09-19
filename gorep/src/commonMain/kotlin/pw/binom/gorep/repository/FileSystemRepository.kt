package pw.binom.gorep.repository

import kotlinx.serialization.json.Json
import pw.binom.ByteBuffer
import pw.binom.DEFAULT_BUFFER_SIZE
import pw.binom.copyTo
import pw.binom.gorep.*
import pw.binom.io.file.*
import pw.binom.io.use
import pw.binom.logger.Logger
import pw.binom.logger.warn

class FileSystemRepository(override val name: String, val root: File) : Repository {
    private val repositoryDir = root

    private val logger = Logger.getLogger("FileSystemRepository $name")

    override suspend fun publish(meta: ArtifactMetaInfo, archive: File) {
        val libDir = repositoryDir
            .relative(meta.name)
            .relative(meta.version.toString())
        libDir.mkdirs()
        val metaFile = libDir.relative(MANIFEST_FILE)
        val dependency = libDir.relative(ARCHIVE_FILE)
        metaFile.rewrite(text = Json.encodeToString(ArtifactMetaInfo.serializer(), meta))
        archive.openRead().use { from ->
            dependency.openWrite(false).use { to ->
                from.copyTo(to)
            }
        }
    }

    override suspend fun download(name: String, version: Version, localCache: LocalCache): ArtifactMetaInfo? {
        val addonDir = repositoryDir
            .relative(name)
            .relative(version.toString())
            .takeIfDirection() ?: return null

        val manifestFile = addonDir.relative(MANIFEST_FILE)
        if (!manifestFile.isFile) {
            logger.warn("Lost manifest file in $addonDir")
            return null
        }
        val archive = addonDir.relative(ARCHIVE_FILE)
        if (!archive.isFile) {
            logger.warn("Lost archive file in $addonDir")
            return null
        }
        val localCacheAddonDir = localCache.root.relative(name).relative(version.toString())
        localCacheAddonDir.mkdirs()
        val localCacheManifestFile = localCacheAddonDir.relative(MANIFEST_FILE)
        val localCacheArchive = localCacheAddonDir.relative(ARCHIVE_FILE)
        manifestFile.copyFile(localCacheManifestFile)
        archive.copyFile(localCacheArchive)
        return ArtifactMetaInfo.readJsonFromText(manifestFile.readText())
    }
}

fun File.copyFile(destination: File, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
    ByteBuffer.alloc(bufferSize).use { buffer ->
        copyFile(destination = destination, buffer = buffer)
    }
}

fun File.copyFile(destination: File, buffer: ByteBuffer) {
    destination.openWrite().use { write ->
        openRead().use { read ->
            while (true) {
                buffer.clear()
                if (read.read(buffer) <= 0) {
                    break
                }
                buffer.flip()
                write.writeFully(buffer)
            }
        }
    }
}