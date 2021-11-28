package pw.binom.gorep.lua

import pw.binom.gorep.Context
import pw.binom.gorep.Project
import pw.binom.gorep.tasks.AbstractTask
import pw.binom.io.file.relative
import pw.binom.io.file.rewrite

class GenerateLuaMetaTask(val context: Context, val project: Project) : AbstractTask() {
    override val name: String
        get() = "gorep_global_lua"
    override val clazz: String
        get() = "info"

    init {
        description = "Generates gorep.lua with gorep global lua field and functions"
    }

    override suspend fun run() {
        val file = project.root.relative("gorep.lua")
        if (file.isDirectory) {
            TODO("Can't create $file")
        }

        file.rewrite(GOREP_GLOBAL)

    }
}

private val GOREP_GLOBAL =
    """--Generated file
---
--- Map with system environment variables
ENV = {}

---
--- Path to project root folder
project_root = ''

---
--- Project name
project_name = ''

---
--- Project version
project_version = ''

---
--- Project Author. Can be nil
project_author = ''

---
--- Os type
os_type = ''

---
--- Path to user directory
user_directory = ''

---
--- Platform type
platform_type = ''

---
--- Adds new task
---@param name name of task
---@param func name of function for new task
---@param class task class. Optional
function add_task(name, func, class) end

---
--- Set new description to task
---@param name name of task for reset description
---@param description text of new description
function set_task_description(name, description) end

---
--- Add dependency to task
---@param task name of task for add dependency
---@param dependency_name name of depend task
function add_task_dependency_by_name(task,dependency_name) end

---
--- Adding all task with class to task as depdendencies
---@param task name of task for add depdendencies
---@param class name of class
function add_task_dependencies_all_by_class(task, class) end

---
--- Applying external file or plugin
--- File should be relative to current file
--- Plugin should contents name and version. For example `my-plugin:1.0`
--- This method is equivalent to `require`
--- Each plugin or file can be apply only once
---@param path relative path to file or plugin
function apply(path) end

---
--- Execute external process
---@param path path to exe. Can be absolute or relative
---@param args array of arguments. For example `{'1', '2', '3'}`. Optional. Default: empty array
---@param env map of environments. For example `{USER='root, VALUE='current_value'}`. Optional. Default: empty map
---@param work_directory path to work directory. Optional. Default value: current work directory
---@param stdin string for pass to process on start. If `stdin==nil` then process will be execute without any stdin
---@param stderr boolean. Set need to grap stderr of process. Optional. Default: true
---@param stdout boolean. Set need to grap stdout of process. Optional. Default: true
function execute_process(path, args, env, work_directory, stdin, stderr, stdout) end

---
--- Returns true of lua started from root apply. And returns false of lua started as a part of other lua plugin.
--- During task running always returns false
---@return Returns true of lua started from root apply. And returns false of lua started as a part of other lua plugin.
function is_main() end
"""