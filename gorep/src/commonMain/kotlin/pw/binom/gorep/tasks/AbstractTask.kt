package pw.binom.gorep.tasks

import pw.binom.gorep.Task
import pw.binom.gorep.TaskSelector
import pw.binom.io.file.File
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class AbstractTask : Task {
    override var description: String? = null
    override var taskSelector: TaskSelector = TaskSelector.NoTasks
    override val inputFiles = ArrayList<File>()
    override val outputFiles = ArrayList<File>()
    override var enabled: Boolean = true
    override var runOnSource: Boolean = false
    private val doFirst = ArrayList<suspend () -> Unit>()
    private val doLast = ArrayList<suspend () -> Unit>()
    abstract suspend fun execute()
    override suspend fun run() {
        doFirst.forEach {
            it()
        }
        execute()
        doLast.forEach {
            it()
        }
    }

    interface Property<T> : ReadOnlyProperty<AbstractTask, T> {
        override fun getValue(thisRef: AbstractTask, property: KProperty<*>): T
    }

    interface ReadonlyProperty<T> : Property<T> {

    }

    interface VarProperty<T> : Property<T>, ReadWriteProperty<AbstractTask, T> {

    }

    fun <T> property(builder: PropertyBuilder<T>.() -> Unit): PropertyDelegateProvider<AbstractTask, Property<T>> {
        val b = PropertyBuilderImpl<T>()
        b.builder()
        return PropertyProvider(
            getter = b.getter ?: throw IllegalArgumentException("getter for property not defined"),
            setter = b.setter
        )
    }

    private val properties = HashMap<String, Property<out Any>>()

    private class PropertyProvider<T>(getter: () -> T, setter: ((T) -> Unit)?) :
        PropertyDelegateProvider<AbstractTask, Property<T>> {

        init {
            if (setter == null) {

            }
        }

        override fun provideDelegate(thisRef: AbstractTask, property: KProperty<*>): Property<T> {
            TODO("Not yet implemented")
        }


    }

    interface PropertyBuilder<T> {
        fun get(func: () -> T)
        fun set(func: (T) -> Unit)
    }

    private class PropertyBuilderImpl<T> : PropertyBuilder<T> {
        var getter: (() -> T)? = null
        var setter: ((T) -> Unit)? = null
        override fun get(func: () -> T) {
            getter = func
        }

        override fun set(func: (T) -> Unit) {
            setter = func
        }

    }

    override fun doFirst(func: suspend () -> Unit) {
        doFirst += func
    }

    override fun doLast(func: suspend () -> Unit) {
        doLast += func
    }
}