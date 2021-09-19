package pw.binom.gorep

import kotlinx.serialization.json.Json
import pw.binom.io.file.*
import pw.binom.io.use

const val BUILD_DIR_NAME = ".build"
const val PROJECT_FILE = "gorep_project.json"
const val GODOT_PROJECT_FILE = "project.godot"
const val ADDONS_DIR = "addons"

class Project private constructor(
    val projectFile: File,
    val pluginDir: File,
    val info: ProjectInfo,
    val root: File,
) : DepUnit {
    companion object {
        fun open(root: File): Project? {
            if (!root.isExist) {
                return null
            }
            if (!root.isDirectory) {
                return null
            }
            val projectFile = root.relative(PROJECT_FILE)
            if (!projectFile.isFile) {
                return null
            }

            val project = Json.decodeFromString(ProjectInfo.serializer(), projectFile.readText())
            project.dependencies.forEach {
                if (it.name == project.name) {
                    TODO("Can't include self")
                }
            }
            val libDir = root.relative(ADDONS_DIR).relative(project.name)

            return Project(
                projectFile = projectFile,
                pluginDir = libDir,
                info = project,
                root = root,
            )
        }
    }

    suspend fun resolveDependencies(depProvider: DepProvider, forceUpdate: Boolean) =
        DependencyResolver.resolve(
            depUnit = this,
            depProvider = depProvider,
            forceUpdate = forceUpdate,
        )

    override val name: String
        get() = info.name
    override val version: Version
        get() = info.version
    override val dependencies: Collection<Dep>
        get() = info.dependencies
}

val Project.addonsDir
    get() = root.relative(ADDONS_DIR)

fun Project.makeAddonsDir() = root.relative(ADDONS_DIR).mkdirs()!!
val Project.buildDir
    get() = root.relative(BUILD_DIR_NAME).takeIfDirection()

fun Project.makeBuildDir() = root.relative(BUILD_DIR_NAME).mkdirs()!!