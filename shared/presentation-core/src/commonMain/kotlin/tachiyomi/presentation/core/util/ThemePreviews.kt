package tachiyomi.presentation.core.util

// CMP port: Android's tooling @Preview with uiMode qualifiers isn't multiplatform; the
// annotation stays as a no-op marker so ported screens keep their ThemePreviews usage.
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ThemePreviews
