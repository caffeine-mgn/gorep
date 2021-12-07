package pw.binom.gorep

import kotlinx.coroutines.Dispatchers
import pw.binom.gorep.repository.FileSystemRepository
import pw.binom.gorep.repository.Repository
import pw.binom.gorep.repository.WebdavRepository
import pw.binom.io.file.File
import pw.binom.network.Network
import pw.binom.network.NetworkCoroutineDispatcher

class ContextImpl private constructor(
    override val verbose: Boolean,
    override val properties: Map<String, String>, override val localCache: LocalCache,
) : Context {

    companion object {
        fun create(
            properties: Map<String, String>,
            verbose: Boolean,
            localCache: LocalCache,
        ): ContextImpl {
//            val variableReplacer = StandardVariableReplacer(properties = properties)
            return ContextImpl(
                localCache = localCache,
                verbose = verbose,
                properties = properties,
            )
        }
    }


    override var status: Context.Status = Context.Status.NEW
}

//override val repositoryService: RepositoryService =
//    RepositoryService(repositories = repositories.values, localCache = localCache)