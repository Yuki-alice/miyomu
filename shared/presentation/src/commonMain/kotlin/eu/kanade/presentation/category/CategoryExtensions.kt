package eu.kanade.presentation.category

import androidx.compose.runtime.Composable
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** CMP port: the Context-based overload is dropped; the composable property is kept. */
val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }
