package pw.binom.gorep

import pw.binom.gorep.repository.Repository

interface RepositoryContainer {
    val repositories: Collection<Repository>
    fun addRepository(repository: Repository)
    fun removeRepository(repository: Repository)
}

class RepositoryContainerImpl : RepositoryContainer {
    override val repositories = HashSet<Repository>()

    override fun addRepository(repository: Repository) {
        if (repositories.any { it.name == repository.name }) {
            throw IllegalArgumentException("Repository ${repository.name} already exist")
        }

    }

    override fun removeRepository(repository: Repository) {
        repositories -= repository
    }

}