package pw.binom.gorep

import pw.binom.compression.tar.TarEntityType
import pw.binom.compression.tar.TarReader
import pw.binom.compression.zlib.GZIPInput
import pw.binom.copyTo
import pw.binom.gorep.tasks.DepInfo
import pw.binom.io.file.*
import pw.binom.io.use

const val MANIFEST_FILE = "manifest.json"
const val ARCHIVE_FILE = "addon.tar.gz"

class LocalCache(val root: File) {

    fun find(name: String, version: Version): ArtifactMetaInfo? {
        val manifest =
            root.relative(name).relative(version.toString()).relative(MANIFEST_FILE).takeIfFile() ?: return null
        return ArtifactMetaInfo.readJsonFromText(manifest.readText())
    }

    /**
     * Install dependency to project
     *
     * @param destination addons folder of project
     * @param name dependency name
     * @param version dependency version
     */
    suspend fun install(destination: File, name: String, version: Version) {
        val f = find(name = name, version = version) ?: TODO()
        destination.mkdirs()
        destination.relative("$name.json").rewrite(DepInfo(sha = f.sha256, version = f.version).toJsonText())
        val addonDir = destination.relative(name)
        addonDir.mkdirs()
        addonDir.list().forEach {
            it.deleteRecursive()
        }
        val addonTarFile = root.relative(name).relative(version.toString()).relative(ARCHIVE_FILE).takeIfFile()?:TODO()
        GZIPInput(addonTarFile.openRead()).let { TarReader(it) }.use {
            while (true) {
                val entity = it.getNextEntity() ?: break
                if (entity.type == TarEntityType.DIRECTORY) {
                    addonDir.relative(entity.name).mkdirs()
                }
                if (entity.type == TarEntityType.NORMAL) {
                    val file = addonDir.relative(entity.name)
                    file.parent!!.mkdirs()
                    file.openWrite().use { fileOutput ->
                        entity.copyTo(fileOutput)
                        fileOutput.flush()
                    }
                }
            }
        }
    }
}