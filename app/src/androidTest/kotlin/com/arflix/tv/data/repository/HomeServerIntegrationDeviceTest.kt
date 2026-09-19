package com.arflix.tv.data.repository

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.arflix.tv.data.telegram.TelegramClient
import com.arflix.tv.data.telegram.TelegramConfig
import com.arflix.tv.network.OkHttpProvider
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class HomeServerIntegrationDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun telegramCredentialsAndNativeLibraryAreAvailable() {
        assertTrue("Telegram credentials missing from APK", TelegramConfig.isConfigured)
        assertTrue("Telegram native library missing from APK", TelegramClient(context).isAvailable)
    }

    @Test fun allThreeServerLibrariesSurviveEncryptedStorageAndLoadOverHttp() = runBlocking {
        LibraryServer().use { server ->
            val profile = "server-test-${UUID.randomUUID()}"
            val profiles = mockk<ProfileManager> {
                every { activeProfileId } returns MutableStateFlow(profile)
                coEvery { getProfileId() } returns profile
                every { profileStringKeyFor(any(), any()) } answers {
                    stringPreferencesKey("${firstArg<String>()}_${secondArg<String>()}")
                }
            }
            val repository = HomeServerRepository(context, OkHttpProvider.client, profiles)
            try {
                val servers = listOf(HomeServerKind.PLEX, HomeServerKind.JELLYFIN, HomeServerKind.EMBY).map { kind ->
                    HomeServerConnection(connectionId = kind.name, serverKind = kind,
                        serverUrl = server.url, accessToken = "fixture-token", userId = "member")
                }
                repository.importCloudConnectionsJsonForProfile(profile, Gson().toJson(servers))
                repository.refreshMissingLibraries()
                // Recreate the repository to exercise reading encrypted settings, not an in-memory result.
                val reopened = HomeServerRepository(context, OkHttpProvider.client, profiles)
                val saved = reopened.currentConnections()
                assertEquals(3, saved.size)
                assertTrue(saved.all { it.isUsable && it.accessToken == "fixture-token" })
                val libraries = reopened.getSavedCatalogCandidates(saved)
                assertEquals(3, libraries.size)
                libraries.forEach { library ->
                    val page = reopened.loadCatalogItems(library.sourceRef, 0, 60, propagateErrors = true)
                    assertEquals("${library.serverKind} returned no library items", 1, page.items.size)
                    assertEquals("Test Film", page.items.single().title)
                    assertEquals(1, page.totalCount)
                }
            } finally {
                repository.importCloudConnectionsJsonForProfile(profile, null)
            }
        }
    }

    private class LibraryServer : AutoCloseable {
        private val socket = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        private val executor = Executors.newSingleThreadExecutor()
        val url = "http://127.0.0.1:${socket.localPort}"
        init {
            executor.submit {
                while (!socket.isClosed) {
                    val client = try { socket.accept() } catch (_: java.io.IOException) { break }
                    client.use {
                        val reader = it.getInputStream().bufferedReader()
                        val path = reader.readLine().split(' ')[1].substringBefore('?')
                        val headers = generateSequence { reader.readLine()?.takeIf(String::isNotEmpty) }.toList()
                        val authenticated = headers.any { line ->
                            (line.startsWith("X-Plex-Token:", true) || line.startsWith("X-Emby-Token:", true)) &&
                                line.substringAfter(':').trim() == "fixture-token"
                        }
                        val body = when (path) {
                            "/library/sections" -> """{"MediaContainer":{"Directory":[{"key":"1","title":"Movies","type":"movie"}]}}"""
                            "/Users/member/Views" -> """{"Items":[{"Id":"1","Name":"Movies","CollectionType":"movies"}]}"""
                            "/library/sections/1/all" -> """{"MediaContainer":{"totalSize":1,"Metadata":[{"ratingKey":"film","title":"Test Film","type":"movie"}]}}"""
                            "/Users/member/Items" -> """{"TotalRecordCount":1,"Items":[{"Id":"film","Name":"Test Film","Type":"Movie"}]}"""
                            else -> "{}"
                        }.toByteArray()
                        val status = if (authenticated) "200 OK" else "401 Unauthorized"
                        it.getOutputStream().apply {
                            write("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(body)
                            flush()
                        }
                    }
                }
            }
        }
        override fun close() {
            socket.close()
            executor.shutdownNow()
        }
    }
}
