package pw.binom.gorep.repository

import pw.binom.gorep.ArtifactMetaInfo
import pw.binom.gorep.LocalCache
import pw.binom.gorep.Version
import pw.binom.io.file.File

interface Repository {
    val name: String

    /**
     * Publishing archive to repository
     * @param meta addon meta info
     * @param archive addon archive tar.gz file
     */
    suspend fun publish(meta: ArtifactMetaInfo, archive: File)
    suspend fun download(name: String, version: Version, localCache: LocalCache): ArtifactMetaInfo?
}