package pw.binom.gorep.lua

import pw.binom.*
import pw.binom.collection.LinkedList
import pw.binom.gorep.Context
import pw.binom.gorep.ProcessRunner
import pw.binom.gorep.Project
import pw.binom.gorep.TaskSelector
import pw.binom.gorep.tasks.LuaTask
import pw.binom.io.file.*
import pw.binom.lua.*
import kotlin.random.Random

class LuaContainer(val project: Project, val context: Context) {
    private val engine = LuaEngine()
    private val funcs = UserFunctionContainer()
    private val workDirectory = LinkedList<File>()
    private val currentWorkDirectory
        get() = workDirectory.getLast()
    private var isMainFlag = false

    init {
        val envTable = LuaValue.of(
            Environment.getEnvs()
                .entries
                .associate { LuaValue.of(it.key) to LuaValue.of(it.value) }
        )


        Environment.currentTimeMillis
        engine.setGlobal("ENV", envTable)
        engine.setGlobal("os_type", LuaValue.of(Environment.os.name))
        engine.setGlobal("platform_type", LuaValue.of(Environment.platform.name))
        engine.setGlobal("user_directory", LuaValue.of(Environment.userDirectory))
        engine.setGlobal("project_root", LuaValue.of(project.root.path))
        engine.setGlobal("project_name", LuaValue.of(project.name))
        engine.setGlobal("project_version", LuaValue.of(project.version.toString()))
        engine.setGlobal("project_author", project.info.author?.let { LuaValue.of(it) } ?: LuaValue.Nil)
    }

    private val alreadyApplyedFile = HashSet<File>()

    private fun applyFile(file: File) {
        if (file in alreadyApplyedFile) {
            return
        }
        val scriptText = file.readText()
        execute(
            script = scriptText,
            workDirectory = file.parent ?: currentWorkDirectory,
            isMain = false,
        )
        alreadyApplyedFile += file
    }

    private fun applyFile(file: String) {
        val scriptFile = File(file).takeIfFile() ?: currentWorkDirectory.relative(file).takeIfFile()
        ?: throw LuaException("File \"$file\" not found")
        applyFile(scriptFile)
    }

    private val isMain = defineFunction("is_main") { input, output ->
        output += LuaValue.of(isMainFlag)
    }

    private fun applyPlugin(file: String) {
        throw LuaException("Plugin not supported")
    }

    private val apply = defineFunction("apply") { input, output ->
        if (input.size != 1) {
            throw LuaException("Invalid arguments")
        }
        val str = input[0].checkedString()
        val localFile = File(str).takeIfFile() ?: currentWorkDirectory.relative(str).takeIfExist()
        if (localFile != null) {
            applyFile(localFile)
            return@defineFunction
        }
        if (":" in str) {
            applyPlugin(str)
            return@defineFunction
        }
        throw LuaException("Unknown module \"$str\"")
    }

    init {
        engine.setGlobal("require", apply)
    }

    private val addTask = defineFunction("add_task") { input, output ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("name", 0).checkedString()
        val functionName = args.get("func", 1).checkedString()
        val taskClass: String = args.get("class", 2).stringOrNull() ?: "lua_task_${Random.nextUuid().toShortString()}"
        val task = LuaTask(
            luaContainer = this,
            functionName = functionName,
            name = taskName,
            clazz = taskClass,
        )
        context.addTask(task)
    }

    private val setTaskDescription = defineFunction("set_task_description") { input, output ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("name", 0).checkedString()
        val description = args.get("description", 1).checkedString()
        context.getTaskByName(taskName).description = description
    }

    private val taskExist = defineFunction("task_exist") { input, output ->
        val taskName = ArgumentReader.create(input).get("name", 0).checkedString()
        output += LuaValue.of(context.tasks.any { it.name == taskName })
    }

    private val executeProcess = defineFunction("execute_process") { input, output ->
        val args = ArgumentReader.create(input)
        val path = args.get("path", 0).checkedString()
        File(path).takeIfFile() ?: currentWorkDirectory.relative(path).takeIfFile()
        ?: throw LuaException("Can't find external process \"$path\"")
        val luaProcessArgs = args.get("args", 1).checkedTable()
        val env = args.get("env", 2).tableOrNull()
            ?.toMap()
            ?.entries
            ?.associate { it.key.checkedString() to it.value.checkedString() }
            ?: emptyMap()
        val workDir = args.get("work_directory", 3).stringOrNull() ?: currentWorkDirectory.path
        val stdin = args.get("stdin", 4).stringOrNull()
        val stderr = args.get("stderr", 5).booleanOrNull() ?: true
        val stdout = args.get("stdout", 6).booleanOrNull() ?: true
        val background = args.get("background", 7).booleanOrNull() ?: false


        val processArgs = luaProcessArgs.toMap().entries
            .asSequence()
            .map {
                val index = it.key.numberOrNull()?.toLong() ?: it.key.intOrNull()
                ?: throw LuaCastException("Can't cast ${it.key} to integer")
                index to it.value.checkedString()
            }
            .sortedBy { it.first }
            .map { it.second }
            .toList()
        val result = ProcessRunner.execute(
            path = path,
            args = processArgs,
            env = env,
            workDir = workDir,
            stdin = stdin,
            stderr = stderr,
            stdout = stdout,
            background = background,
        )
        output += if (result == null) {
            LuaValue.Nil
        } else {
            LuaValue.of(result)
        }
    }

    private val addTaskDependencyByName = defineFunction("add_task_dependency_by_name") { input, output ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("task", 0).checkedString()
        val dependencyName = args.get("dependency_name", 1).checkedString()
        val task = context.getTaskByName(taskName)
        task.taskSelector = task.taskSelector + TaskSelector.SelectedTask(context.getTaskByName(dependencyName))
    }

    private val addTaskAllDependenciesByClass = defineFunction("add_task_dependencies_all_by_class") { input, output ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("task", 0).checkedString()
        val className = args.get("class", 1).checkedString()
        val task = context.getTaskByName(taskName)
        task.taskSelector = task.taskSelector + TaskSelector.CustomSelector { it.clazz == className }
    }

    fun execute(script: String, workDirectory: File, isMain: Boolean): List<LuaValue> {
        this.workDirectory.add(workDirectory)
        val oldIsMain = isMainFlag
        isMainFlag = isMain
        try {
            return engine.eval(script)
        } finally {
            isMainFlag = oldIsMain
            this.workDirectory.removeLast()
        }
    }

    fun call(functionName: String, vararg args: LuaValue): List<LuaValue> =
        engine.call(functionName, *args)

    private fun defineFunction(name: String, func: LuaFunction): LuaValue.Function {
        val value = funcs.add(func)
        engine.setGlobal(name, value)
        return value
    }
}