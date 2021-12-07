package pw.binom.gorep

import kotlinx.coroutines.Dispatchers
import pw.binom.gorep.repository.FileSystemRepository
import pw.binom.gorep.repository.WebdavRepository
import pw.binom.io.file.File
import pw.binom.network.Network

object RepositoryPlugin:Plugin {
    override suspend fun apply(project: Project) {
        project.info.repositories.forEach {
            val repo = when (it.type) {
                RepositoryType.LOCAL -> FileSystemRepository(name = it.name, root = File(it.path))
                RepositoryType.WEBDAV -> WebdavRepository.create(
                    networkDispatcher = Dispatchers.Network,
                    name = it.name,
                    uri = it.path,
                    auth = it.basicAuth,
                    variableReplacer = StandardVariableReplacer(project.context.properties),
                    verbose = project.context.verbose,
                )
            }
            project.addRepository(repo)
        }
    }
}