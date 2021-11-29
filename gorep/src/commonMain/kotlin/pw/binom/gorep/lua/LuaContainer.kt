package pw.binom.gorep.lua

import pw.binom.*
import pw.binom.collection.LinkedList
import pw.binom.gorep.*
import pw.binom.gorep.tasks.LuaTask
import pw.binom.io.file.*
import pw.binom.lua.*
import kotlin.random.Random

class LuaContainer(val project: Project, val context: Context) {
    private val engine = LuaEngine()
    private val funcs = ObjectContainer()
    private val workDirectory = LinkedList<File>()
    private val currentWorkDirectory
        get() = workDirectory.getLast()
    private var isMainFlag = false

    init {
        engine["ENV"] = LuaValue.of(
            Environment.getEnvs()
                .entries
                .associate { it.key.lua to it.value.lua }
        )
        engine["os_type"] = Environment.os.name.lua
        engine["platform_type"] = Environment.platform.name.lua
        engine["user_directory"] = Environment.userDirectory.lua
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

    private val addTaskInput = defineFunction("add_task_input") {
        val args = ArgumentReader.create(it)
        val task = args.get("task", 0).checkedString()
        val file = args.get("file", 1).checkedString()
        context.getTaskByName(task).inputFiles += File(file)
        emptyList()
    }

    private val addTaskOutput = defineFunction("add_task_output") {
        val args = ArgumentReader.create(it)
        val task = args.get("task", 0).checkedString()
        val file = args.get("file", 1).checkedString()
        context.getTaskByName(task).outputFiles += File(file)
        emptyList()
    }

    private val setTaskEnabled = defineFunction("set_task_enabled") {
        val args = ArgumentReader.create(it)
        val task = args.get("task", 0).checkedString()
        val enabled = args.get("enabled", 1).checkedBoolean()
        context.getTaskByName(task).enabled = enabled
        emptyList()
    }

    private val isMain = defineFunction("is_main") {
        listOf(isMainFlag.lua)
    }

    private fun applyPlugin(file: String) {
        throw LuaException("Plugin not supported")
    }

    private val getProjectInfo = defineFunction("get_project_info") {
        listOf(
            LuaValue.of(
                mapOf(
                    "addons_dir".lua to project.addonsDir.path.lua,
                    "root_dir".lua to project.root.path.lua,
                    "name".lua to project.name.lua,
                    "version".lua to project.version.toString().lua,
                    "author".lua to (project.info.author?.lua ?: LuaValue.Nil),
                )
            )
        )
    }

    private val apply = defineFunction("apply") { input ->
        if (input.size != 1) {
            throw LuaException("Invalid arguments")
        }
        val str = input[0].checkedString()
        val localFile = File(str).takeIfFile() ?: currentWorkDirectory.relative(str).takeIfExist()
        if (localFile != null) {
            applyFile(localFile)
            return@defineFunction emptyList()
        }
        if (":" in str) {
            applyPlugin(str)
            return@defineFunction emptyList()
        }
        throw LuaException("Unknown module \"$str\"")
    }

    init {
        engine["require"] = apply
    }

    private val addTask = defineFunction("add_task") { input ->
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
        emptyList()
    }

    private val setTaskDescription = defineFunction("set_task_description") { input ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("name", 0).checkedString()
        val description = args.get("description", 1).checkedString()
        context.getTaskByName(taskName).description = description
        emptyList()
    }

    private val taskExist = defineFunction("task_exist") { input ->
        val taskName = ArgumentReader.create(input).get("name", 0).checkedString()
        listOf(context.tasks.any { it.name == taskName }.lua)
    }

    private val executeProcess = defineFunction("execute_process") { input ->
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
        listOf(result?.lua ?: LuaValue.Nil)
    }

    private val addTaskDependencyByName = defineFunction("add_task_dependency_by_name") { input ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("task", 0).checkedString()
        val dependencyName = args.get("dependency_name", 1).checkedString()
        val task = context.getTaskByName(taskName)
        task.taskSelector = task.taskSelector + TaskSelector.SelectedTask(context.getTaskByName(dependencyName))
        emptyList()
    }

    private val addTaskAllDependenciesByClass = defineFunction("add_task_dependencies_all_by_class") { input ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("task", 0).checkedString()
        val className = args.get("class", 1).checkedString()
        val task = context.getTaskByName(taskName)
        task.taskSelector = task.taskSelector + TaskSelector.CustomSelector { it.clazz == className }
        emptyList()
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

    private fun defineFunction(name: String, func: LuaFunction): LuaValue.FunctionValue {
        val value = funcs.makeClosure(func)
        engine[name] = value
        return value
    }
}