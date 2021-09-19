package pw.binom.gorep.repository

import pw.binom.copyTo
import pw.binom.gorep.*
import pw.binom.io.bufferedWriter
import pw.binom.io.file.*
import pw.binom.io.httpClient.HttpClient
import pw.binom.io.use
import pw.binom.net.URI
import pw.binom.net.toPath
import pw.binom.network.NetworkDispatcher
import pw.binom.webdav.BasicAuthorization
import pw.binom.webdav.client.WebDavClient

class WebdavRepository(
    val networkDispatcher: NetworkDispatcher,
    override val name: String,
    val uri: URI,
    val auth: BasicAuth?,
) : Repository {
    private val client = HttpClient.create(networkDispatcher)
    private val webDavclient = WebDavClient(client = client, url = uri)

    private val user =
        if (auth == null) {
            null
        } else {
            BasicAuthorization(login = auth.login, password = auth.password)
        }

    override suspend fun publish(meta: ArtifactMetaInfo, archive: File) {
        if (!archive.isFile) {
            throw FileNotFoundException(archive.toString())
        }
        val dir = "/${meta.name}/${meta.version}".toPath
        webDavclient.useUser(user = user) {
            webDavclient.mkdirs(dir)
                ?: TODO("Can't create webdav dir")
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

    override suspend fun download(name: String, version: Version, localCache: LocalCache): ArtifactMetaInfo? {
        val found = webDavclient.useUser(user = user) {
            val manifest = webDavclient.get("/$name/$version/$MANIFEST_FILE".toPath)?:return@useUser false
            val archive = webDavclient.get("/$name/$version/$ARCHIVE_FILE".toPath)?:return@useUser false
            val localCacheAddonDir = localCache.root.relative(name).relative(version.toString())
            localCacheAddonDir.mkdirs()
            manifest.read()!!.use { input ->
                localCacheAddonDir.relative(MANIFEST_FILE).openWrite().use { output ->
                    input.copyTo(output)
                }
            }
            archive.read()!!.use { input ->
                localCacheAddonDir.relative(ARCHIVE_FILE).openWrite().use { output ->
                    input.copyTo(output)
                }
            }
            true
        }
        if (!found){
            return null
        }
        return localCache.find(name = name, version = version)!!
    }
}

//suspend fun FileSystem.mkdirs(path: String) {
//    val items = path.split("/")
//    val sb = StringBuilder("/")
//    var first = true
//    items.forEach { currentDir ->
//        val current = getDir(currentDir)
//        if (current==null){
//            mkdir(currentDir)
//        }
//        if (!first) {
//            sb.append("/")
//        }
//        sb.append(currentDir)
//        first = false
//    }
//}