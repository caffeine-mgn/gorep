package pw.binom.gorep

import pw.binom.async2
import pw.binom.concurrency.joinAndGetOrThrow
import kotlin.test.Test

class DependencyResolverTest {
    @Test
    fun test() {
        val root = module(name = "main") {
            dep("cursor", version = "1.0", type = DepType.INTERNAL)
            dep("windows", version = "1.0")
        }
        val library = library {
            lib("cursor", version = "1.0") {
                dep("os")
            }
            lib("cursor", version = "1.1") {
                dep("os")
            }
            lib("windows", version = "1.0") {
                dep("cursor", version = "1.1", type = DepType.EXTERNAL)
            }
            lib("os")
        }
        val r = async2 {
            DependencyResolver.resolve(
                root,
                library,
                forceUpdate = false
            )
        }.joinAndGetOrThrow()
        printTree(r)
    }
}

fun printTree(d: DepUnitW) {
    val finished = HashSet<DepUnitW>()
    var duplicate = false
    fun dd(mod: DepUnitW, depType: DepType, oldType: DepType, v: Version, tab: Int) {
        repeat(tab) {
            print(" ")
        }
        print("${mod.name}:")
        if (v.compareTo(mod.version) != 0) {
            print("$v->${mod.version}")
        } else {
            print("${mod.version}")
        }
        if (depType != oldType) {
            print(" $oldType -> $depType")
        } else {
            print(" $depType")
        }

        val alreadyFinished = mod in finished
        if (alreadyFinished) {
            print(" *")
            duplicate = true
        }
        finished += mod
        println()
        if (!alreadyFinished) {
            mod.deps.forEach {
                dd(mod = it.module!!.depUnit, depType = it.type, oldType = it.oldType, v = it.version, tab = tab + 1)
            }
        }
    }
    dd(mod = d, depType = DepType.EXTERNAL, oldType = DepType.EXTERNAL, v = d.version, tab = 0)
    if (duplicate) {
        println("\n\"*\" marked dependency already printed")
    }
}

fun module(name: String, version: String = "1.0", func: (ModuleBuilder.() -> Unit)? = null): DepUnit {
    val builder = ModuleBuilder()
    if (func != null) {
        builder.func()
    }
    return object : DepUnit {
        override val name: String = name
        override val version: Version = Version(version)
        override val dependencies: Collection<Dep>
            get() = builder.deps
    }
}

fun library(func: LibBuilder.() -> Unit): DepProvider {
    val l = LibBuilder()
    l.func()
    return l
}

class LibBuilder : DepProvider {
    val libs = ArrayList<DepUnit>()
    fun lib(name: String, version: String = "1.0", func: (ModuleBuilder.() -> Unit)? = null) {
        libs += module(name = name, version = version, func = func)
    }

    override suspend fun get(name: String, version: Version, forceUpdate: Boolean): DepUnit? =
        libs.find {
            it.name == name && it.version.compareTo(version) == 0
        }
}

class ModuleBuilder {
    val deps = ArrayList<Dep>()
    fun dep(name: String, version: String = "1.0", type: DepType = DepType.EXTERNAL) {
        deps += object : Dep {
            override val name: String = name
            override val version: Version = Version(version)
            override val type: DepType = type
        }
    }
}