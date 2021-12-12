package pw.binom.gorep.lua

import pw.binom.concurrency.SpinLock
import pw.binom.concurrency.synchronize
import pw.binom.gorep.Project
import pw.binom.gorep.Task
import pw.binom.gorep.tasks.LuaTask2
import pw.binom.io.Closeable
import pw.binom.io.file.File
import pw.binom.lua.*
import pw.binom.nextUuid
import kotlin.random.Random

object LuaProjectTask {
    fun define(project: Project, engine: LuaEngine, o: ObjectContainer) {

    }
}

object LuaTaskSupport {
    fun define(project: Project, engine: LuaEngine, o: ObjectContainer) {
        val addTask = o.makeClosure { args ->

            listOf()
        }
        project.luaContainer
    }
}

fun interface LFunction<SELF> {
    fun invoke(self: SELF, args: List<LuaValue>): List<LuaValue>
}

fun interface LConstructor<SELF> {
    fun new(args: List<LuaValue>): SELF
}

interface LuaClass<T : Any> {
    val methods: Map<String, LFunction<T>>
    val constructor: LConstructor<T>?
}

fun <T : Any> LuaClass<T>.toLua(self: T, engine: LuaEngine, o: ObjectContainer) {
    engine.createUserData(LuaValue.LightUserData(self))
}

class Definer(val engine: LuaEngine, val o: ObjectContainer) {
    private val lock = SpinLock()

    private class Item(val metatable: LuaValue.TableRef, val closable: Closeable) : Closeable {
        override fun close() {
            closable.close()
        }

    }

    private val cleaners = HashMap<LuaClass<out Any>, Item>()
    fun clear() = lock.synchronize<Unit> {
        cleaners.values.forEach {
            it.close()
        }
        cleaners.clear()
    }

    fun <T : Any> undefine(clazz: LuaClass<T>) = lock.synchronize<Unit> {
        cleaners.remove(clazz)?.close()
    }

    private fun <T : Any> wrap2(clazz: LuaClass<T>, self: T): LuaValue.UserData {
        val item = cleaners[clazz] ?: throw IllegalStateException("Class ${clazz::class} is not defined")
        val ud = engine.createUserData(LuaValue.LightUserData(self))
        ud.metatable = item.metatable
        return ud
    }

    fun <T : Any> wrap(clazz: LuaClass<T>, self: T) = lock.synchronize {
        wrap2(clazz, self)
    }

    fun <T : Any> define(clazz: LuaClass<T>) = lock.synchronize<Unit> {
        val functions = clazz.methods.entries.associate { (name, func) ->
            val funcRef = o.makeClosure { args ->
                val self = args[0].checkedData().value as T
                val funcArgs = args.subList(1, args.lastIndex)
                func.invoke(self, funcArgs)
            }
            name.lua to funcRef
        }
        val classBody = engine.makeRef(LuaValue.TableValue(functions as Map<LuaValue, LuaValue>))

        val projectMetatable = engine.makeRef(
            LuaValue.TableValue(
                "__index".lua to classBody,
                "__gc".lua to engine.closureAutoGcFunction,
            )
        )
        val constructorFunctionRef = if (clazz.constructor != null) {
            val funcRef = o.makeClosure { args ->
                val obj = clazz.constructor!!.new(args)
                val ud = engine.createUserData(LuaValue.LightUserData(obj))
                ud.metatable = projectMetatable
                listOf(ud)
            }
            classBody["new".lua] = funcRef
            funcRef
        } else {
            null
        }
        cleaners[clazz] = Item(
            metatable = projectMetatable,
            closable = {
                if (constructorFunctionRef != null) {
                    o.removeClosure(constructorFunctionRef)
                }
                functions.values.forEach {
                    o.removeClosure(it)
                }
            }
        )
    }
}

abstract class BaseLuaClass<T : Any> : LuaClass<T> {
    override val methods = HashMap<String, LFunction<T>>()
    override var constructor: LConstructor<T>? = null

    protected fun func(name: String, func: LFunction<T>): LFunction<T> {
        methods[name] = func
        return func
    }
}

class LuaProjectWrapper(
    val project: Project,
) {
    companion object {
        fun wrap(project: Project, engine: LuaEngine, gcMetatable: LuaValue.TableRef): LuaValue.UserData {
            val u = engine.createUserData(LuaValue.LightUserData(LuaProjectWrapper(project)))
            u.metatable = gcMetatable
            return u
        }

        fun apply(
            parentTable: LuaValue.TableValue,
            project: Project,
            engine: LuaEngine,
            o: ObjectContainer,
            gcMetatable: LuaValue.TableRef
        ) {
            parentTable["get_project".lua] = o.makeClosure { args ->
                listOf(wrap(project, engine = engine, gcMetatable = gcMetatable))
            }
            parentTable["get_project_name".lua] = o.makeClosure { args ->
                listOf(args[1].checkedData().value<LuaProjectWrapper>().project.name.lua)
            }

            parentTable["get_project_task_by_name".lua] = o.makeClosure { args ->
                val args = ArgumentReader.create(args, true)
                val project = args.get("project", 0).checkedData().value<LuaProjectWrapper>().project
                val name = args.get("name", 1).checkedString()
                val task = project.getTaskByName(name)
                listOf(
                    LuaTaskWrapper.wrap(
                        task = task,
                        engine = engine,
                        gcMetatable = gcMetatable
                    )
                )
            }
        }
    }
}

class LuaTaskWrapper(val task: Task) {
    companion object {
        fun wrap(task: Task, engine: LuaEngine, gcMetatable: LuaValue.TableRef): LuaValue.UserData {
            val u = engine.createUserData(LuaValue.LightUserData(LuaTaskWrapper(task)))
            u.metatable = gcMetatable
            return u
        }

        fun apply(
            parentTable: LuaValue.TableValue,
            project: Project,
            engine: LuaEngine,
            o: ObjectContainer,
            gcMetatable: LuaValue.TableRef
        ) {
            parentTable["add_task_input".lua] = o.makeClosure { args ->
                val args = ArgumentReader.create(args, true)
                val project = args.get("project", 0).checkedData().value<LuaProjectWrapper>().project
                val task = args.get("task", 1).checkedString()
                val file = args.get("file", 2).checkedString()
                project.getTaskByName(task).inputFiles += File(file)
                emptyList()
            }

            parentTable["add_task_output".lua] = o.makeClosure { args ->
                val args = ArgumentReader.create(args, true)
                val project = args.get("project", 0).checkedData().value<LuaProjectWrapper>().project
                val task = args.get("task", 1).checkedString()
                val file = args.get("file", 2).checkedString()
                project.getTaskByName(task).outputFiles += File(file)
                emptyList()
            }
            parentTable["doFirst".lua] = o.makeClosure { args ->
                val args = ArgumentReader.create(args, true)
                val task = args.get("task", 0).checkedData().value<LuaTaskWrapper>().task
                emptyList()
            }
            parentTable["create_task".lua] = o.makeClosure { args ->
                val args = ArgumentReader.create(args, true)
                val project = args.get("project", 0).checkedData().value<LuaProjectWrapper>().project
                val taskName = args.get("name", 1).checkedString()
                val action = args.get("action", 2)
                val taskClass: String =
                    args.get("class", 2).stringOrNull() ?: "lua_task_${Random.nextUuid().toShortString()}"
                val task = LuaTask2(
                    name = taskName,
                    clazz = taskClass,
                    engine = engine,
                    executeFunc = action.takeIfNotNil
                )
                project.addTask(task)
                listOf(
                    wrap(
                        task = task,
                        engine = engine,
                        gcMetatable = gcMetatable
                    )
                )
            }
            parentTable["set_task_description".lua] = o.makeClosure { args ->
                val args = ArgumentReader.create(args, true)
                val task = args.get("task", 0).checkedData().value<LuaTaskWrapper>().task
                val description = args.get("description", 1).checkedString()
                task.description = description
                emptyList()
            }
        }
    }
}
