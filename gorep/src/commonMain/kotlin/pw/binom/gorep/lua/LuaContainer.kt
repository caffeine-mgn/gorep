package pw.binom.gorep.lua

import pw.binom.*
import pw.binom.collection.LinkedList
import pw.binom.gorep.*
import pw.binom.io.file.*
import pw.binom.lua.*

class LuaContainer(val project: Project) {
    private val engine = LuaEngine()
    private val funcs = ObjectContainer()
    private val workDirectory = LinkedList<File>()
    private val currentWorkDirectory
        get() = workDirectory.getLast()
    private var isMainFlag = false
    private val definer = Definer(engine, funcs)
    private val gcMetatable = engine.makeRef(
        LuaValue.TableValue().also {
            it["__gc".lua] = engine.userdataAutoGcFunction
        }
    )

    init {
        engine["ENV"] = LuaValue.of(
            Environment.getEnvs()
                .entries
                .associate { it.key.lua to it.value.lua }
        )
        engine["os_type"] = Environment.os.name.lua
        engine["platform_type"] = Environment.platform.name.lua
        engine["user_directory"] = Environment.userDirectory.lua
        val gorepTable = LuaValue.TableValue()
        LuaProjectWrapper.apply(
            parentTable = gorepTable,
            project = project,
            engine = engine,
            o = funcs,
            gcMetatable = gcMetatable
        )
        LuaTaskWrapper.apply(
            parentTable = gorepTable,
            project = project,
            engine = engine,
            o = funcs,
            gcMetatable = gcMetatable
        )
        engine["gorep"] = gorepTable

        engine["os"].checkedTable()["name".lua] = funcs.makeClosure { args ->
            listOf(Environment.os.name.lua)
        }

        engine["os"].checkedTable()["executable_extension".lua] = funcs.makeClosure { args ->
            listOf(EXE_SUFIX.lua)
        }

        engine["os"].checkedTable()["build_directory".lua] = funcs.makeClosure { args ->
            listOf(project.buildDir.path.lua)
        }

        engine["os"].checkedTable()["find_program".lua] = funcs.makeClosure { args ->
            val programName = args[0].checkedString()
            val path = Environment.getEnv("PATH")
                ?.split(PATH_SEPARATOR)
                ?.asSequence()
                ?.map { File(it).relative(programName) }
                ?.filter { it.isFile }
                ?.firstOrNull()
            if (path != null) {
                listOf(path.path.lua)
            } else {
                emptyList()
            }
        }
    }

    private val PATH_SEPARATOR = if (Environment.os == OS.WINDOWS) ';' else ':'
    private val EXE_SUFIX = if (Environment.os == OS.WINDOWS) ".exe" else ""

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

    private val setTaskEnabled = defineFunction("set_task_enabled") {
        val args = ArgumentReader.create(it)
        val task = args.get("task", 0).checkedString()
        val enabled = args.get("enabled", 1).checkedBoolean()
        project.getTaskByName(task).enabled = enabled
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

    private val taskExist = defineFunction("task_exist") { input ->
        val taskName = ArgumentReader.create(input).get("name", 0).checkedString()
        listOf(project.tasks.any { it.name == taskName }.lua)
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
        val task = project.getTaskByName(taskName)
        task.taskSelector = task.taskSelector + TaskSelector.SelectedTask(project.getTaskByName(dependencyName))
        emptyList()
    }

    private val addTaskAllDependenciesByClass = defineFunction("add_task_dependencies_all_by_class") { input ->
        val args = ArgumentReader.create(input)
        val taskName = args.get("task", 0).checkedString()
        val className = args.get("class", 1).checkedString()
        val task = project.getTaskByName(taskName)
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

    fun call(functionName: LuaValue, vararg args: LuaValue): List<LuaValue> =
        engine.call(functionName, *args)

    private fun defineFunction(name: String, func: LuaFunction): LuaValue {
        val value = engine.createACClosure(func)
        engine[name] = value
        return value
    }
}