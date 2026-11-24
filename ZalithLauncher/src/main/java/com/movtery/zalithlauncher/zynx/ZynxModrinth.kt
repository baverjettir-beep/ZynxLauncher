/*
 * Zynx Launcher - Modrinth helper (search, compatible version resolve, download)
 * Based on Zalith Launcher 2 (GPL-3.0)
 */

package com.movtery.zalithlauncher.zynx

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.installed.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** What kind of content the library screen manages. */
enum class ZynxKind(val projectType: String, val folderName: String, val extension: String) {
    MOD("mod", "mods", "jar"),
    RESOURCE_PACK("resourcepack", "resourcepacks", "zip"),
    SHADER("shader", "shaderpacks", "zip");

    companion object {
        fun fromIndex(index: Int): ZynxKind = values().getOrElse(index) { MOD }
    }
}

data class ZynxHit(
    val projectId: String,
    val slug: String,
    val title: String,
    val description: String,
    val author: String,
    val downloads: Long
)

data class ZynxResolved(
    val fileName: String,
    val url: String,
    val dependencies: List<String>
)

class ZynxReport {
    val installed = mutableListOf<String>()
    val skipped = mutableListOf<String>()
}

object ZynxModrinth {
    private const val BASE = "https://api.modrinth.com/v2"
    private const val USER_AGENT = "ZynxLauncher/1.0 (https://github.com/baverjettir-beep/ZynxLauncher)"

    /** Modrinth loader ids that can run on the given loader. */
    fun loadersOf(loader: ModLoader?): List<String> = when (loader) {
        ModLoader.FORGE -> listOf("forge")
        ModLoader.NEOFORGE -> listOf("neoforge")
        ModLoader.FABRIC, ModLoader.LEGACY_FABRIC -> listOf("fabric")
        ModLoader.QUILT -> listOf("quilt", "fabric")
        else -> emptyList()
    }

    fun minecraftVersionOf(version: Version): String? = version.getVersionInfo()?.minecraftVersion

    fun loaderOf(version: Version): ModLoader? = version.getVersionInfo()?.primaryLoader?.loader

    fun folderOf(version: Version, kind: ZynxKind): File =
        File(version.getGameDir(), kind.folderName).also { it.mkdirs() }

    /** Performance mods installed by ZynxModPack (Modrinth slugs). */
    fun modPackSlugs(loader: ModLoader?): List<String> = when (loader) {
        ModLoader.FABRIC, ModLoader.QUILT, ModLoader.LEGACY_FABRIC ->
            listOf("fabric-api", "sodium", "lithium", "ferrite-core", "modernfix", "entityculling", "immediatelyfast")
        ModLoader.FORGE, ModLoader.NEOFORGE ->
            listOf("embeddium", "ferrite-core", "modernfix", "entityculling", "immediatelyfast")
        else -> emptyList()
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun search(
        kind: ZynxKind,
        query: String,
        mcVersion: String?,
        loaders: List<String>,
        offset: Int = 0
    ): List<ZynxHit> = withContext(Dispatchers.IO) {
        val facets = JSONArray()
        facets.put(JSONArray().put("project_type:${kind.projectType}"))
        if (!mcVersion.isNullOrBlank()) facets.put(JSONArray().put("versions:$mcVersion"))
        if (kind == ZynxKind.MOD && loaders.isNotEmpty()) {
            val any = JSONArray()
            loaders.forEach { any.put("categories:$it") }
            facets.put(any)
        }
        val index = if (query.isBlank()) "downloads" else "relevance"
        val url = "$BASE/search?query=${enc(query)}&facets=${enc(facets.toString())}" +
            "&limit=20&offset=$offset&index=$index"
        val hits = JSONObject(httpGet(url)).getJSONArray("hits")
        val result = ArrayList<ZynxHit>()
        for (i in 0 until hits.length()) {
            val h = hits.getJSONObject(i)
            result.add(
                ZynxHit(
                    projectId = h.getString("project_id"),
                    slug = h.optString("slug"),
                    title = h.optString("title"),
                    description = h.optString("description"),
                    author = h.optString("author"),
                    downloads = h.optLong("downloads")
                )
            )
        }
        result
    }

    /**
     * Finds the newest version of [project] that matches the game version (and loader for mods).
     * Prefers release builds, falls back to beta/alpha.
     */
    suspend fun resolve(
        project: String,
        kind: ZynxKind,
        mcVersion: String?,
        loaders: List<String>
    ): ZynxResolved? = withContext(Dispatchers.IO) {
        val params = StringBuilder()
        if (!mcVersion.isNullOrBlank()) {
            params.append("game_versions=").append(enc(JSONArray().put(mcVersion).toString()))
        }
        if (kind == ZynxKind.MOD && loaders.isNotEmpty()) {
            if (params.isNotEmpty()) params.append('&')
            params.append("loaders=").append(enc(JSONArray(loaders).toString()))
        }
        val url = "$BASE/project/${enc(project)}/version" + if (params.isNotEmpty()) "?$params" else ""
        val versions = JSONArray(httpGet(url))
        if (versions.length() == 0) return@withContext null

        var chosen: JSONObject? = null
        for (i in 0 until versions.length()) {
            val v = versions.getJSONObject(i)
            if (v.optString("version_type") == "release") {
                chosen = v
                break
            }
        }
        val version = chosen ?: versions.getJSONObject(0)

        val files = version.optJSONArray("files")
        if (files == null || files.length() == 0) return@withContext null
        var file = files.getJSONObject(0)
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            if (f.optBoolean("primary")) {
                file = f
                break
            }
        }

        val deps = ArrayList<String>()
        val depArray = version.optJSONArray("dependencies")
        if (depArray != null) {
            for (i in 0 until depArray.length()) {
                val d = depArray.getJSONObject(i)
                if (d.optString("dependency_type") == "required" && !d.isNull("project_id")) {
                    deps.add(d.getString("project_id"))
                }
            }
        }
        ZynxResolved(
            fileName = file.getString("filename"),
            url = file.getString("url"),
            dependencies = deps
        )
    }

    suspend fun download(url: String, dest: File) {
        withContext(Dispatchers.IO) {
            val tmp = File(dest.parentFile, dest.name + ".part")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            try {
                if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
                conn.inputStream.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
                if (!tmp.renameTo(dest)) throw IOException("Could not save ${dest.name}")
            } finally {
                conn.disconnect()
                if (tmp.exists()) tmp.delete()
            }
        }
    }

    /**
     * Installs [project] (plus its required dependencies for mods) into [targetDir].
     * If the newest file is incompatible, the newest compatible one is picked automatically.
     */
    suspend fun install(
        project: String,
        kind: ZynxKind,
        mcVersion: String?,
        loaders: List<String>,
        targetDir: File,
        report: ZynxReport,
        visited: MutableSet<String> = mutableSetOf(),
        depth: Int = 0,
        onStatus: (String) -> Unit = {}
    ) {
        if (!visited.add(project)) return
        onStatus(project)
        val resolved = try {
            resolve(project, kind, mcVersion, loaders)
        } catch (e: Exception) {
            null
        }
        if (resolved == null) {
            report.skipped.add(project)
            return
        }
        val dest = File(targetDir, File(resolved.fileName).name)
        try {
            if (!dest.exists()) download(resolved.url, dest)
            report.installed.add(dest.name)
        } catch (e: Exception) {
            report.skipped.add(project)
            return
        }
        if (kind == ZynxKind.MOD && depth < 3) {
            for (dep in resolved.dependencies) {
                install(dep, kind, mcVersion, loaders, targetDir, report, visited, depth + 1, onStatus)
            }
        }
    }

    /** One-tap performance pack for the given version. */
    suspend fun installModPack(version: Version, onStatus: (String) -> Unit = {}): ZynxReport {
        val report = ZynxReport()
        val loader = loaderOf(version)
        val loaders = loadersOf(loader)
        val mc = minecraftVersionOf(version)
        val dir = folderOf(version, ZynxKind.MOD)
        val visited = mutableSetOf<String>()
        for (slug in modPackSlugs(loader)) {
            install(slug, ZynxKind.MOD, mc, loaders, dir, report, visited, 0, onStatus)
        }
        return report
    }
}
