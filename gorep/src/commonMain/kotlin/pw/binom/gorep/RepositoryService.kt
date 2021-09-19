package pw.binom.gorep

import pw.binom.gorep.repository.Repository

class RepositoryService(val repositories: Collection<Repository>, val localCache: LocalCache) : DepProvider {

//    fun getSha256(name: String, version: Version, updateCache: Boolean = false): String =
//        localRepository.getSha256(name = name, version = version)
//            ?: throw IOException("Dependency ${name}:${version} not found")
//
//    fun store(projectRoot: File, name: String, version: Version, updateCache: Boolean = false): Boolean {
//        if (!localRepository.store(
//                projectRoot = projectRoot,
//                name = name,
//                version = version,
//            )
//        ) {
//            throw IOException("Dependency ${name}:${version} not found")
//        }
//        return true
//    }

//    fun getMeta(name: String, version: Version): ArtifactMetaInfo? {
//        val meta = localRepository.getMeta(name = name, version = version)
//        if (meta != null) {
//            return meta
//        }
//
//        throw IOException("Dependency ${name}:${version} not found")
//    }

    override suspend fun get(name: String, version: Version, forceUpdate: Boolean): DepUnit? {
        val local = if (forceUpdate) null else localCache.find(name = name, version = version)
        if (local != null) {
            return local
        }
        repositories.forEach {
            val remote = it.download(name = name, version = version, localCache = localCache)
            if (remote != null) {
                return remote
            }
        }
        return null
    }

}