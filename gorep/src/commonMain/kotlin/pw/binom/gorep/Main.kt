package pw.binom.gorep

import kotlinx.serialization.json.Json
import pw.binom.*
import pw.binom.io.file.File
import pw.binom.io.file.mkdirs
import pw.binom.io.file.relative
import pw.binom.io.file.rewrite
import kotlin.native.concurrent.SharedImmutable
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import pw.binom.gorep.lua.LuaPlugin
import pw.binom.lua.LuaEngine
import pw.binom.lua.ObjectContainer

fun printHelp() {
    println("Help:")
    println("not implemented")
}

@OptIn(ExperimentalTime::class)
suspend fun runInProject(args: List<String>, rootProject: Project, context: ContextImpl) {
    val totalDuration = measureTime {
        runSuspendFunc {


            context.status = Context.Status.TASKS_RESOLVE

            RepositoryPlugin.apply(project = rootProject)
            BasePlugin.apply(project = rootProject)
            GodotPlugin.apply(project = rootProject)
            LuaPlugin.apply(project = rootProject)
            val taskRunner = TaskController.resolve(rootProject)
            context.status = Context.Status.FINISH

            val taskForExecute = args.map {
                rootProject.tasks.find { e -> e.name == it }
                    ?: throw RuntimeException("Can't find task \"$it\"")
            }
            taskRunner.run(taskForExecute)

//            val executed = HashSet<Task>()
//
//            suspend fun run(task: Task) {
//                if (task in executed) {
//                    return
//                }
//                executed += task
//                val dependencies = task.getDependencies()
//                dependencies.forEach {
//                    run(it)
//                }
//                println("> Task ${task.name}")
//                task.run()
//            }
//
//
//            args.forEach { arg ->
//                val task = context.tasks.find { it.name == arg } ?: TODO("Task \"$arg\" not found")
//                run(task)
//            }
        }
    }
    println("SUCCESSFUL in $totalDuration")
}

@OptIn(ExperimentalTime::class)
fun runWithoutProject(args: List<String>, properties: Map<String, String>, verbose: Boolean) {
    val argsStack = Stack<String>()
    args.forEach {
        argsStack.pushLast(it)
    }
    val workDirectory = File(Environment.workDirectory)
    val taskForExecute = ArrayList<Task2>()
    LOOP@ while (!argsStack.isEmpty) {
        for (it in tasks2) {
            val newTask = it.config(workDirectory = workDirectory, args = argsStack) ?: continue
            taskForExecute += newTask
            continue@LOOP
        }
        if (args.isEmpty()) {
            break@LOOP
        }
        throw RuntimeException("Unknown argument ${argsStack.peekFirst()}")
    }
    val totalDuration = measureTime {
        taskForExecute.forEach { task ->
            println("> Task ${task.name}")
            task.execute()
        }
    }
    println("SUCCESSFUL in $totalDuration")
}

fun <T> MutableCollection<T>.removeIf(func: (T) -> Boolean) {
    val it = iterator()
    while (it.hasNext()) {
        if (func(it.next())) {
            it.remove()
        }
    }
}

fun printVersion() {
    println("Gorep $GOREP_VERSION")
}

fun main(args: Array<String>) {

    val localCachePath =
        Environment.getEnv("GOREP_LOCAL_CACHE") ?: "${Environment.userDirectory}${File.SEPARATOR}/.gorep"
    val localCacheFile =
        File(localCachePath).mkdirs()
            ?: throw RuntimeException("Can't create local cache by path \"$localCachePath\"")
    val localCache = LocalCache(localCacheFile)
    val argsList = args.toMutableList()

    if (argsList.size == 1 && "--version" in argsList) {
        printVersion()
        return
    }

    val properties = HashMap<String, String>()
    argsList.removeIf {
        if (it.startsWith("-D")) {
            val items = it.split('=', limit = 2)
            properties[items[0].substring(2)] = items.getOrNull(1) ?: ""
            return@removeIf true
        }
        false
    }
//    val properties1 = args.filter { it.startsWith("-D") }
//        .map { it.removePrefix("-D") }
//        .map {
//            val items = it.split('=', limit = 2)
//            items[0] to (items.getOrNull(1) ?: "")
//        }
//        .toMap()
    var verbose = false
    argsList.removeIf {
        if (it == "-v" || it == "--verbose") {
            verbose = true
            return@removeIf true
        }
        false
    }
    val context = ContextImpl.create(
        properties = properties,
        verbose = verbose,
        localCache = localCache,
    )

    runSuspendFunc {
        val project = Project.open(context = context, root = File(Environment.workDirectory))
        if (project != null) {
            runInProject(args = argsList, rootProject = project, context = context)
        } else {
            runWithoutProject(args = argsList, properties = properties, verbose = verbose)
        }
    }
}

interface Task2Factory {
    fun config(workDirectory: File, args: Stack<String>): Task2?
}

interface Task2 {
    val name: String
    fun execute()
}

object InitTaskFactory : Task2Factory {
    override fun config(workDirectory: File, args: Stack<String>): Task2? {
        if (args.peekFirst() == "init") {
            args.popFirst()
            return InitTask(workDirectory)
        }
        return null
    }
}

val prettyJson = Json {
    this.prettyPrint = true
}

class InitTask(val directory: File) : Task2 {
    override val name: String
        get() = "init"

    override fun execute() {
        val info = ProjectInfo(
            name = "init",
            title = "StartUp",
            version = Version("0.0.1"),
            dependencies = emptyList(),
        )
        val json = prettyJson.encodeToString(ProjectInfo.serializer(), info)
        directory.relative(PROJECT_FILE).rewrite(json)
    }

}

@SharedImmutable
val tasks2 = listOf(InitTaskFactory)