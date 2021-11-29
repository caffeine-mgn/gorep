package pw.binom.gorep

import kotlinx.serialization.Serializable
import pw.binom.Stack

object DependencyResolver {
    suspend fun resolve(depUnit: DepUnit, depProvider: DepProvider, forceUpdate: Boolean): DepUnitW {
        val resolve = Stack<DepW>()
        val versions = HashMap<String, DepWRef>()

        fun addDep(d: DepUnit, type: DepType): DepUnitW {
            val dd = DepUnitW(d, type)
            d.dependencies.forEach {
                val w = DepW(it)
                dd.deps += w
                resolve.pushLast(w)
            }
            return dd
        }

        val root = addDep(depUnit, DepType.EXTERNAL)

        while (!resolve.isEmpty) {
            val e = resolve.popFirst()
            val v = versions[e.dep.name]
            if (v == null) {
                val newVersion = depProvider.get(name = e.name, version = e.version, forceUpdate = forceUpdate)
                    ?: throw RuntimeException("Can't find dependency ${e.name}:${e.version}")
                val r = DepWRef(addDep(newVersion, e.type))
                versions[e.dep.name] = r
                e.module = r
            } else {
                val type = e.type.getUpper(v.depUnit.type)
                if (e.dep.version > v.depUnit.unit.version) {
                    val newVersion = depProvider.get(name = e.name, version = e.version, forceUpdate = forceUpdate)
                        ?: throw RuntimeException("Can't find dependency ${e.name}:${e.version}")
                    v.depUnit = addDep(newVersion, type)
                    //нужно апнуть версию
                } else {
                    v.depUnit.type = type
                }
                e.module = v
            }
        }
        checkCycleAllDep(root)
        return root
    }
}

class DepWRef(var depUnit: DepUnitW)

class DepUnitW(val unit: DepUnit, var type: DepType) : DepUnit {
    val deps = ArrayList<DepW>()
    override val name: String
        get() = unit.name
    override val version: Version
        get() = unit.version
    override val dependencies: Collection<Dep>
        get() = deps
    override val lua: String?
        get() = unit.lua

    fun flatAllDependencies(): List<Dep> {
        val parsed = HashSet<DepUnitW>()
        val forParse = ArrayList<DepUnitW>()
        this.deps.forEach {
            forParse += it.module.depUnit
        }
        while (forParse.isNotEmpty()) {
            val l = forParse.removeLast()
            if (l === this) {
                continue
            }
            if (parsed.add(l)) {
                l.deps.forEach {
                    forParse += it.module.depUnit
                }
            }
        }

        return parsed.map {
            ProjectDependency(name = it.name, version = it.version, type = it.type)
        }
    }
}

private fun checkCycleOneDep(dep: DepUnitW) {
    val alreadyChecked = HashSet<DepUnitW>()
    val forCheck = Stack<DepUnitW>()

    dep.deps.forEach {
        forCheck.pushLast(it.module.depUnit)
        alreadyChecked += it.module.depUnit
    }
    while (!forCheck.isEmpty) {
        val e = forCheck.popFirst()
        if (e == dep) {
            throw RuntimeException("${dep.name}:${dep.version} has cycle dependency")
        }
        e.deps.forEach {
            if (it.module.depUnit !in alreadyChecked) {
                alreadyChecked += it.module.depUnit
                forCheck.pushLast(it.module.depUnit)
            }
        }
    }
}

private fun checkCycleAllDep(root: DepUnitW) {
    val alreadyChecked = HashSet<DepUnitW>()
    val forCheck = Stack<DepUnitW>()
    forCheck.pushFirst(root)

    while (!forCheck.isEmpty) {
        val e = forCheck.popFirst()
        checkCycleOneDep(e)
        e.deps.forEach {
            if (it.module.depUnit !in alreadyChecked) {
                alreadyChecked += it.module.depUnit
                forCheck.pushLast(it.module.depUnit)
            }
        }
    }
}

class DepW(val dep: Dep) : Dep {
    lateinit var module: DepWRef
    override val name: String
        get() = dep.name
    override val version: Version
        get() = dep.version
    override val type: DepType
        get() = newType ?: oldType
    val oldType
        get() = dep.type
    val newType
        get() = if (this::module.isInitialized) {
            module.depUnit.type
        } else {
            null
        }
}

interface DepProvider {
    suspend fun get(name: String, version: Version, forceUpdate: Boolean): DepUnit?
}

interface DepUnit {
    val name: String
    val version: Version
    val dependencies: Collection<Dep>
    val lua: String?
}

interface Dep {
    val name: String
    val version: Version
    val type: DepType
}

@Serializable
enum class DepType(val order: Int) {
    INTERNAL(order = 1), EXTERNAL(order = 2);

    fun getUpper(depType: DepType) =
        if (this.order < depType.order) depType else this
}