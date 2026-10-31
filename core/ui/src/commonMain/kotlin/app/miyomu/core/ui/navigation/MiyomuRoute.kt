package app.miyomu.core.ui.navigation

/** Lightweight route model. Kept independent from any navigation library. */
sealed interface MiyomuRoute {
    val key: String

    data object Home : MiyomuRoute {
        override val key: String = "home"
    }

    data object Library : MiyomuRoute {
        override val key: String = "library"
    }

    data object Browse : MiyomuRoute {
        override val key: String = "browse"
    }

    data object Updates : MiyomuRoute {
        override val key: String = "updates"
    }

    data object More : MiyomuRoute {
        override val key: String = "more"
    }

    data class Details(val sourceName: String, val contentId: String) : MiyomuRoute {
        override val key: String = "details/$sourceName/$contentId"
    }

    data class Reader(
        val sourceName: String,
        val contentId: String,
        val chapterId: String,
    ) : MiyomuRoute {
        override val key: String = "reader/$sourceName/$contentId/$chapterId"
    }
}
