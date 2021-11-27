package pw.binom.gorep.repository

import pw.binom.AsyncInput
import pw.binom.copyTo
import pw.binom.gorep.*
import pw.binom.io.bufferedWriter
import pw.binom.io.file.*
import pw.binom.io.httpClient.HttpClient
import pw.binom.io.httpClient.create
import pw.binom.io.use
import pw.binom.net.URI
import pw.binom.net.toPath
import pw.binom.net.toURI
import pw.binom.network.NetworkCoroutineDispatcher
import pw.binom.webdav.BasicAuthorization
import pw.binom.webdav.client.WebDavClient

class WebdavRepository private constructor(
    val networkDispatcher: NetworkCoroutineDispatcher,
    override val name: String,
    val uri: URI,
    val user: BasicAuthorization?,
    val verbose: Boolean,
) : Repository {
    companion object {
        suspend fun create(
            networkDispatcher: NetworkCoroutineDispatcher,
            name: String,
            uri: String,
            auth: BasicAuth?,
            variableReplacer: VariableReplacer,
            verbose: Boolean,
        ): WebdavRepository {
            val url = (variableReplacer.replace(uri) ?: uri).toURI()
            val user = auth?.let {
                BasicAuthorization(
                    login = it.getLogin(variableReplacer),
                    password = it.getPassword(variableReplacer),
                )
            }
            return WebdavRepository(
                networkDispatcher = networkDispatcher,
                name = name,
                uri = url,
                user = user,
                verbose = verbose,
            )
        }
    }

    private val client = HttpClient.create(networkDispatcher)
    private val webDavclient = WebDavClient(client = client, url = uri)

    override suspend fun publish(meta: ArtifactMetaInfo, archive: File) {
        if (!archive.isFile) {
            throw FileNotFoundException(archive.toString())
        }
        val dir = "/${meta.name}/${meta.version}".toPath
        webDavclient.useUser(user = user) {
            webDavclient.mkdirs(dir)
                ?: TODO("Can't create webdav dir")
            webDavclient.getDir(dir)?.forEach {
                if (it.isFile && it.name != MANIFEST_FILE && it.name != ARCHIVE_FILE) {
                    it.delete()
                }
            }
            webDavclient.new(dir.append(MANIFEST_FILE)).bufferedWriter().use {
                it.append(meta.toJson())
            }
            webDavclient.new(dir.append(ARCHIVE_FILE)).use { out ->
                archive.openRead().use { input ->
                    input.copyTo(out)
                }
            }
        }
    }

    override suspend fun publishMeta(name: String, version: Version, fileName: String, input: AsyncInput) {
        val dir = "/${name}/${version}".toPath
        webDavclient.useUser(user = user) {
            webDavclient.new(dir.append(fileName, direction = true)).use { output ->
                input.copyTo(output)
            }
        }
    }

    override suspend fun download(name: String, version: Version, localCache: LocalCache): ArtifactMetaInfo? {
        val found = webDavclient.useUser(user = user) {
            val manifest = webDavclient.get("/$name/$version/$MANIFEST_FILE".toPath) ?: return@useUser false
            val archive = webDavclient.get("/$name/$version/$ARCHIVE_FILE".toPath) ?: return@useUser false
            val localCacheAddonDir = localCache.root.relative(name).relative(version.toString())
            localCacheAddonDir.mkdirs()
            manifest.read()!!.use { input ->
                localCacheAddonDir.relative(MANIFEST_FILE).openWrite().use { output ->
                    input.copyTo(output)
                }
            }
            try {
                archive.read()!!.use { input ->
                    localCacheAddonDir.relative(ARCHIVE_FILE).openWrite().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Throwable) {
                throw RuntimeException("Can't download $archive", e)
            }
            true
        }
        if (!found){
            return null
        }
        return localCache.find(name = name, version = version)!!
    }
}