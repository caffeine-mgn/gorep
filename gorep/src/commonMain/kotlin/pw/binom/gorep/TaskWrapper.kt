package pw.binom.gorep

//interface TaskWrapper<out T : Task> {
//    fun get(): T
//    fun configure(func: (T) -> Unit)
//    fun executeConfiguration()
//    fun doFirst(func: suspend () -> Unit)
//    fun doLast(func: suspend () -> Unit)
//}
//
//
//class TaskProviderImpl<T : Task> : TaskWrapper<T> {
//    private var task: T? = null
//    fun resolve(task: T) {
//        if (this.task != null) {
//            throw IllegalStateException("Task already resolved")
//        }
//        this.task = task
//        doFirst.forEach { task.doFirst(it) }
//        doLast.forEach { task.doLast(it) }
//        doFirst.clear()
//        doLast.clear()
//    }
//
//    override fun get(): T = task ?: throw IllegalStateException("Task not resolved")
//
//    private var configurators = ArrayList<(T) -> Unit>()
//    private var doFirst = ArrayList<suspend () -> Unit>()
//    private var doLast = ArrayList<suspend () -> Unit>()
//
//    override fun configure(func: (T) -> Unit) {
//        configurators += func
//    }
//
//    override fun executeConfiguration() {
//        val task = get()
//        configurators.forEach {
//            it(task)
//        }
//    }
//
//    override fun doFirst(func: suspend () -> Unit) {
//        doFirst += func
//    }
//
//    override fun doLast(func: suspend () -> Unit) {
//        doLast += func
//    }
//}