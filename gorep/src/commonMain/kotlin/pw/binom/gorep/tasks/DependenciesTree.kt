package pw.binom.gorep.tasks

import pw.binom.gorep.*

class DependenciesTree(val context: Context, val project: Project) : Task {
    override val name: String
        get() = "dependencies"

    override val description: String
        get() = "Prints dependencies tree"

    override fun getDependencies(): List<Task> = emptyList()

    override suspend fun run() {
        val dependencies = project.resolveDependencies(context.repositoryService, forceUpdate = false)
        printTree(dependencies)
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
                    dd(
                        mod = it.module!!.depUnit,
                        depType = it.type,
                        oldType = it.oldType,
                        v = it.version,
                        tab = tab + 1
                    )
                }
            }
        }
        dd(mod = d, depType = DepType.EXTERNAL, oldType = DepType.EXTERNAL, v = d.version, tab = 0)
        if (duplicate) {
            println("\n\"*\" marked dependency already printed")
        }
    }
}