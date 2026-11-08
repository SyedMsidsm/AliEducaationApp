package com.alieducational.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.alieducational.app.R

@Composable
fun AliLogo(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    Image(
        painter = painterResource(if (isDark) R.drawable.ali_logo_dark else R.drawable.ali_logo_light),
        contentDescription = "Ali Educational logo",
        contentScale = contentScale,
        modifier = modifier
    )
}
