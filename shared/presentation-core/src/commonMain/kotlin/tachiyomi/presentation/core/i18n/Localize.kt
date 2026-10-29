package tachiyomi.presentation.core.i18n

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.pluralStringResource as mokoPluralStringResource
import dev.icerock.moko.resources.compose.stringResource as mokoStringResource

// CMP port: moko's compose helpers replace the Android Context-based helpers from
// tachiyomi.core.common.i18n so every ported screen keeps calling stringResource(MR.strings.x).
// (No @ReadOnlyComposable: moko's compose helpers read from a State, not the snapshot directly.)
@Composable
fun stringResource(resource: StringResource): String {
    return mokoStringResource(resource)
}

@Composable
fun stringResource(resource: StringResource, vararg args: Any): String {
    return mokoStringResource(resource, *args)
}

@Composable
fun pluralStringResource(resource: PluralsResource, count: Int): String {
    return mokoPluralStringResource(resource, count)
}

@Composable
fun pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String {
    return mokoPluralStringResource(resource, count, *args)
}
