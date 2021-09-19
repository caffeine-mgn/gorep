package pw.binom.gorep

import pw.binom.gorep.repository.FileSystemRepository
import pw.binom.gorep.repository.Repository
import pw.binom.gorep.repository.WebdavRepository
import pw.binom.io.file.File
import pw.binom.net.toURI
import pw.binom.network.NetworkDispatcher

class ContextImpl(val networkDispatcher: NetworkDispatcher, val project: Project, val localCache: LocalCache) :
    Context {
    var status = Status.NEW

    enum class Status {
        NEW,
        TASKS_RESOLVE,
        FINISH
    }

    val repositories = HashMap<Repository2, Repository>()

    init {
        project.info.repositories.forEach {
            val repo = when (it.type) {
                RepositoryType.LOCAL -> FileSystemRepository(name = it.name, root = File(it.path))
                RepositoryType.WEBDAV -> WebdavRepository(
                    networkDispatcher = networkDispatcher,
                    name = it.name,
                    uri = it.path.toURI(),
                    auth = it.basicAuth
                )
            }
            repositories[it] = repo
        }
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