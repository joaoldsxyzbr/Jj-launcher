package dev.jj.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    @Test fun matchesWithAccentsAndCase() {
        assertTrue(appMatchesSearch("Câmera", "CAMERA"))
        assertTrue(appMatchesSearch("Configurações", "configuracoes"))
        assertTrue(appMatchesSearch("Google Fotos", "fotos"))
    }

    @Test fun unrelatedAppsDoNotMatch() {
        assertFalse(appMatchesSearch("Calendário", "telefone"))
    }

    @Test fun normalizesSpaces() {
        assertEquals("aplicativos", normalizeForSearch("  Aplicativos  "))
    }

    @Test fun emptyQueryMatchesAnyApp() {
        assertTrue(appMatchesSearch("Galeria", ""))
    }
}
