package pw.binom.gorep

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import pw.binom.net.URI
import pw.binom.net.toURI

@Serializable
data class ProjectInfo(
    /**
     * Project name. Should same name with name in addons folder
     */
    val name: String,

    /**
     * Addon title. Will be display in Godot addons manager
     */
    val title: String = name,

    /**
     * Plugin description
     */
    val description: String? = null,

    /**
     * Plugin version
     */
    val version: Version = Version("0.1"),

    /**
     * Path to startup file. Direction defined relative to /addons/<plugin name>/ direction
     */
    val script: String = "plugin.gd",

    /**
     * Plugin author
     */
    val author: String? = null,

    /**
     * Addon dependencies
     */
    val dependencies: List<ProjectDependency> = emptyList(),
    val excludes: List<String> = emptyList(),
    val repositories: List<Repository2> = emptyList(),
)

@Serializable
data class Repository2(
    val name: String,
    val path: String,
    val type: RepositoryType,
    val publish: Boolean = false,
    val basicAuth: BasicAuth? = null,
)

@Serializable
data class BasicAuth(
    val login: String,
    val password: String,
)

@Serializable
enum class RepositoryType {
    LOCAL,
    WEBDAV,
}

object URISerializer : KSerializer<URI> {
    override fun deserialize(decoder: Decoder): URI =
        decoder.decodeString().toURI()

    override val descriptor: SerialDescriptor
        get() = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }
}

@Serializable
data class ProjectDependency(
    /**
     * Dependency name
     */
    override val name: String,

    /**
     * Dependency version
     */
    override val version: Version,

    /**
     * Dependency type
     */
    override val type: DepType = DepType.EXTERNAL
) : Dep