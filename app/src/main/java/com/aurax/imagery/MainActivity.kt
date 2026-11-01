package com.aurax.imagery

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurax.imagery.ui.AuraXRoot
import com.aurax.imagery.ui.theme.AuraXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent system bars so the AMOLED background and glass bar run edge to edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val glassSettings = (application as AuraXApp).container.glassSettings
        setContent {
            val glassStyle by glassSettings.style.collectAsStateWithLifecycle()
            AuraXTheme(glassStyle = glassStyle) {
                AuraXRoot()
            }
        }
        // Refresh the index on a fresh launch (not on rotation). The worker itself checks the
        // media permission, so this is safe before the user has granted anything.
        if (savedInstanceState == null) {
            (application as AuraXApp).container.indexer.enqueue()
        }
    }
}
