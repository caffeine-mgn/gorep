package pw.binom.gorep

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import pw.binom.gorep.lua.LuaContainer
import pw.binom.gorep.repository.FileSystemRepository
import pw.binom.gorep.repository.WebdavRepository
import pw.binom.io.file.*
import pw.binom.io.use
import pw.binom.network.Network
import pw.binom.network.NetworkCoroutineDispatcher

const val BUILD_DIR_NAME = ".build"
const val PROJECT_FILE = "gorep_project.json"
const val GODOT_PROJECT_FILE = "project.godot"
const val ADDONS_DIR = "addons"

class Project private constructor(
    val projectFile: File,
    val pluginDir: File,
    val info: ProjectInfo,
    val root: File,
    val taskContainer: TaskContainer,
    val context: Context,
) : DepUnit, TaskContainer by taskContainer, RepositoryContainer by RepositoryContainerImpl() {
    companion object {
        suspend fun open(
            networkDispatcher: NetworkCoroutineDispatcher = Dispatchers.Network,
            context: Context,
            root: File
        ): Project? {
            if (!root.isExist) {
                return null
            }
            if (!root.isDirectory) {
                return null
            }
            val projectFile = root.relative(PROJECT_FILE)
            if (!projectFile.isFile) {
                return null
            }

            val project = Json.decodeFromString(ProjectInfo.serializer(), projectFile.readText())
            project.dependencies.forEach {
                if (it.name == project.name) {
                    TODO("Can't include self")
                }
            }
            val libDir = root.relative(ADDONS_DIR).relative(project.name)

            val p = Project(
                projectFile = projectFile,
                pluginDir = libDir,
                info = project,
                root = root,
                taskContainer = TaskContainerImpl(),
                context = context,
            )


            return p
        }
    }

    val internalLuaContainer = if (info.lua == null) null else LuaContainer(this)
    val luaContainer: LuaContainer
        get() = internalLuaContainer ?: throw IllegalStateException("Project dont't use lua script")
    val repositoryService = RepositoryService(repositories,context.localCache)

//    suspend fun resolveDependencies(depProvider: DepProvider, forceUpdate: Boolean) =
//        DependencyResolver.resolve(
//            depUnit = this,
//            depProvider = depProvider,
//            forceUpdate = forceUpdate,
//        )

    override var name: String = info.name
    override var version: Version = info.version
    override val dependencies: Collection<Dep>
        get() = info.dependencies
    override val lua: String? = info.lua

    override fun addTask(task: Task): Task {
        if (context.status != Context.Status.TASKS_RESOLVE) {
            throw IllegalStateException()
        }
        return taskContainer.addTask(task)
    }
}

val Project.addonsDir
    get() = root.relative(ADDONS_DIR)

fun Project.makeAddonsDir() = root.relative(ADDONS_DIR).mkdirs()!!
val Project.buildDir
    get() = root.relative(BUILD_DIR_NAME)

fun Project.makeBuildDir() = root.relative(BUILD_DIR_NAME).mkdirs()!!