package pw.binom.gorep

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
data class ArtifactMetaInfo(
    override val name: String,
    val title: String,
    override val version: Version,
    val sha256: String,
    override val dependencies: List<Dependency> = emptyList(),

    val metas: List<Meta> = emptyList()
) : DepUnit {
    companion object {
        fun readJsonFromText(text: String) = Json.decodeFromString(serializer(), text)
    }

    fun toJson() = Json.encodeToString(serializer(), this)
}

@Serializable
data class Dependency(
    override val name: String,
    override val version: Version,
) : Dep {
    @Transient
    override val type: DepType
        get() = DepType.EXTERNAL
}