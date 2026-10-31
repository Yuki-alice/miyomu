package app.miyomu.desktop

import app.miyomu.core.ui.navigation.MiyomuNavigator
import app.miyomu.core.ui.navigation.MiyomuRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavigatorTest {
    @Test
    fun pushPopKeepsBackStack() {
        val navigator = MiyomuNavigator()
        assertEquals(MiyomuRoute.Home, navigator.current)
        assertFalse(navigator.canGoBack)

        navigator.push(MiyomuRoute.Details("sky-archive"))
        assertEquals(MiyomuRoute.Details("sky-archive"), navigator.current)
        assertTrue(navigator.canGoBack)

        assertTrue(navigator.pop())
        assertEquals(MiyomuRoute.Home, navigator.current)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun selectTopLevelClearsStack() {
        val navigator = MiyomuNavigator()
        navigator.push(MiyomuRoute.Details("a"))
        navigator.selectTopLevel(MiyomuRoute.Library)
        assertEquals(MiyomuRoute.Library, navigator.current)
        assertFalse(navigator.canGoBack)
    }
}
