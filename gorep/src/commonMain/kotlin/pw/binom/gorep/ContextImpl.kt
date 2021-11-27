package pw.binom.gorep

import kotlinx.coroutines.Dispatchers
import pw.binom.gorep.repository.FileSystemRepository
import pw.binom.gorep.repository.Repository
import pw.binom.gorep.repository.WebdavRepository
import pw.binom.io.file.File
import pw.binom.network.Network
import pw.binom.network.NetworkCoroutineDispatcher

class ContextImpl private constructor(
    val networkDispatcher: NetworkCoroutineDispatcher,
    val project: Project,
    val localCache: LocalCache,
    val repositories: HashMap<Repository2, Repository>,
    override val variableReplacer: VariableReplacer,
    override val verbose: Boolean,
) :
    Context {

    companion object {
        suspend fun create(
                networkDispatcher: NetworkCoroutineDispatcher = Dispatchers.Network,
                project: Project,
                localCache: LocalCache,
                properties: Map<String, String>,
                verbose: Boolean,
        ): ContextImpl {
            val variableReplacer = StandardVariableReplacer(properties = properties)
            val repositories = HashMap<Repository2, Repository>()
            project.info.repositories.forEach {
                val repo = when (it.type) {
                    RepositoryType.LOCAL -> FileSystemRepository(name = it.name, root = File(it.path))
                    RepositoryType.WEBDAV -> WebdavRepository.create(
                        networkDispatcher = networkDispatcher,
                        name = it.name,
                        uri = it.path,
                        auth = it.basicAuth,
                        variableReplacer = variableReplacer,
                        verbose=verbose,
                    )
                }
                repositories[it] = repo
            }
            return ContextImpl(
                networkDispatcher = networkDispatcher,
                project = project,
                localCache = localCache,
                repositories = repositories,
                variableReplacer = variableReplacer,
                verbose = verbose,
            )
        }
    }

    var status = Status.NEW

    enum class Status {
        NEW,
        TASKS_RESOLVE,
        FINISH
    }

    private val _tasks = ArrayList<Task>()
    override val tasks: List<Task>
        get() = _tasks
    override val repositoryService: RepositoryService =
        RepositoryService(repositories = repositories.values, localCache = localCache)

    override fun addTask(task: Task) {
        if (status != Status.TASKS_RESOLVE) {
            throw IllegalStateException()
        }
        _tasks += task
    }
}