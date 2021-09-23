package pw.binom.gorep.repository

import pw.binom.AsyncInput
import pw.binom.gorep.ArtifactMetaInfo
import pw.binom.gorep.LocalCache
import pw.binom.gorep.Version
import pw.binom.io.file.File

interface Repository {
    val name: String

    /**
     * Publishing archive to repository.
     * If artifact override old version all old meta-addon-info files will removed.
     *
     * @param meta addon meta info
     * @param archive addon archive tar.gz file
     */
    suspend fun publish(meta: ArtifactMetaInfo, archive: File)

    /**
     * Publish meta-addon-info file
     */
    suspend fun publishMeta(name: String, version: Version, fileName:String, input:AsyncInput)
    suspend fun download(name: String, version: Version, localCache: LocalCache): ArtifactMetaInfo?
}