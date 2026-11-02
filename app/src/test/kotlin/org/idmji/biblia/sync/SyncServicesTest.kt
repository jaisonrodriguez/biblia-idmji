package org.idmji.biblia.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncServicesTest {

    @Test
    fun testGitHubVersionComparison() {
        val service = GitHubUpdateService()

        // Versiones más nuevas
        assertTrue(service.isNewerVersion("v1.0.1", "1.0"))
        assertTrue(service.isNewerVersion("v1.1.0", "1.0.0"))
        assertTrue(service.isNewerVersion("v2.0.0", "1.9.9"))
        assertTrue(service.isNewerVersion("1.0.1", "1.0.0"))

        // Versiones iguales o más viejas
        assertFalse(service.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(service.isNewerVersion("1.0", "1.0"))
        assertFalse(service.isNewerVersion("v0.9.9", "1.0"))
        assertFalse(service.isNewerVersion("v1.0.0", "1.0.1"))
    }

    @Test
    fun testGitHubReleaseJsonParsing() {
        val service = GitHubUpdateService()
        val mockJson = """
            {
              "tag_name": "v1.1.0",
              "name": "Biblia IDMJI v1.1.0",
              "body": "Nuevas funciones de búsqueda agregadas.",
              "assets": [
                {
                  "name": "Biblia_IDMJI.apk",
                  "browser_download_url": "https://github.com/jaisonrodriguez/biblia-idmji/releases/download/v1.1.0/Biblia_IDMJI.apk"
                }
              ]
            }
        """.trimIndent()

        val result = service.parseReleaseJson(mockJson, "1.0")
        assertTrue(result is UpdateCheckResult.Available)
        val available = result as UpdateCheckResult.Available
        assertEquals("v1.1.0", available.info.latestVersion)
        assertEquals("https://github.com/jaisonrodriguez/biblia-idmji/releases/download/v1.1.0/Biblia_IDMJI.apk", available.info.downloadUrl)
        assertTrue(available.info.isUpdateAvailable)
    }

    @Test
    fun testTursoResponseParsing() {
        val service = TursoSyncService()
        val mockTursoJson = """
            {
              "results": [
                {
                  "type": "ok",
                  "response": {
                    "type": "execute",
                    "result": {
                      "cols": [{"name": "id"}],
                      "rows": [
                        [
                          {"type": "integer", "value": "25000"},
                          {"type": "text", "value": "CANONICO"},
                          {"type": "integer", "value": "1"},
                          {"type": "integer", "value": "1"},
                          {"type": "integer", "value": "1"},
                          {"type": "null"},
                          {"type": "text", "value": "Génesis 1:1"},
                          {"type": "text", "value": "En el principio"},
                          {"type": "text", "value": "https://youtube.com/watch?v=xyz"},
                          {"type": "text", "value": "xyz"},
                          {"type": "text", "value": "estudios_biblicos"},
                          {"type": "text", "value": "Transcripción de prueba"}
                        ]
                      ]
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val items = service.parseTursoRows(mockTursoJson)
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(25000, item.id)
        assertEquals("CANONICO", item.tipo)
        assertEquals(1, item.libroId)
        assertEquals(1, item.capitulo)
        assertEquals(1, item.versiculoInicio)
        assertEquals(null, item.versiculoFin)
        assertEquals("Génesis 1:1", item.cita)
        assertEquals("En el principio", item.titulo)
        assertEquals("https://youtube.com/watch?v=xyz", item.videoUrl)
        assertEquals("xyz", item.videoId)
        assertEquals("estudios_biblicos", item.seccion)
    }

    @Test
    fun testTursoCountParsing() {
        val service = TursoSyncService()
        val mockCountJson = """
            {
              "results": [
                {
                  "type": "ok",
                  "response": {
                    "type": "execute",
                    "result": {
                      "cols": [{"name": "COUNT(*)"}],
                      "rows": [
                        [
                          {"type": "integer", "value": "8653"}
                        ]
                      ]
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val count = service.parseCountResult(mockCountJson)
        assertEquals(8653, count)
    }
}
