package pw.binom.gorep

import kotlinx.serialization.json.Json
import pw.binom.*
import pw.binom.io.file.File
import pw.binom.io.file.mkdirs
import pw.binom.io.file.relative
import pw.binom.io.file.rewrite
import pw.binom.network.NetworkDispatcher
import kotlin.native.concurrent.SharedImmutable
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

fun printHelp() {
    println("Help:")
    println("not implemented")
}

@OptIn(ExperimentalTime::class)
fun runInProject(args: Array<String>, project: Project, properties: Map<String, String>) {
    val nt = NetworkDispatcher()
    val totalDuration = measureTime {
        nt.runSingle {
            val localCachePath =
                Environment.getEnv("GOREP_LOCAL_CACHE") ?: "${Environment.userDirectory}${File.SEPARATOR}/.gorep"
            val localCacheFile =
                File(localCachePath).mkdirs()
                    ?: throw RuntimeException("Can't create local cache by path \"$localCachePath\"")
            val context = ContextImpl.create(
                project = project,
                localCache = LocalCache(localCacheFile),
                networkDispatcher = nt,
                properties = properties,
            )
            context.status = ContextImpl.Status.TASKS_RESOLVE

            BasePlugin.apply(project = project, context = context)
            GodotPlugin.apply(project = project, context = context)
            context.status = ContextImpl.Status.FINISH

            val tc = TaskController(context)
            val taskForExecute = args.map {
                context.tasks.find { e->e.name==it }
                    ?:throw RuntimeException("Can't find task \"$it\"")
            }
            tc.run(taskForExecute)

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
fun runWithoutProject(args: Array<String>, properties: Map<String, String>) {
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

fun main(args: Array<String>) {
    val properties = args.filter { it.startsWith("-D") }
        .map { it.removePrefix("-D") }
        .map {
            val items = it.split('=', limit = 2)
            items[0] to (items.getOrNull(1) ?: "")
        }
        .toMap()
    val argsWithoutProperties = args.filter { !it.startsWith("-D") }.toTypedArray()
    val project = Project.open(File(Environment.workDirectory))
    if (project != null) {
        runInProject(args = argsWithoutProperties, project = project, properties = properties)
    } else {
        runWithoutProject(args = argsWithoutProperties, properties = properties)
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