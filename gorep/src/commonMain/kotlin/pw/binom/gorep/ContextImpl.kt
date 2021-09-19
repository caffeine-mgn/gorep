package pw.binom.gorep

import pw.binom.gorep.repository.FileSystemRepository
import pw.binom.gorep.repository.Repository
import pw.binom.gorep.repository.WebdavRepository
import pw.binom.io.file.File
import pw.binom.net.toURI
import pw.binom.network.NetworkDispatcher

class ContextImpl private constructor(
    val networkDispatcher: NetworkDispatcher,
    val project: Project,
    val localCache: LocalCache,
    val repositories: HashMap<Repository2, Repository>,
    override val variableReplacer: VariableReplacer
) :
    Context {

    companion object {
        suspend fun create(
            networkDispatcher: NetworkDispatcher,
            project: Project,
            localCache: LocalCache,
            properties: Map<String, String>,
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
                        variableReplacer = variableReplacer
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