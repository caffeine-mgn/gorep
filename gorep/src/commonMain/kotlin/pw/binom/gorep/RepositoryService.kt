package pw.binom.gorep

import pw.binom.gorep.repository.Repository

class RepositoryService(val repositories: Collection<Repository>, val localCache: LocalCache) : DepProvider {

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