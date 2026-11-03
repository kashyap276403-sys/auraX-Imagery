package com.aurax.imagery

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.os.SystemClock
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aurax.imagery.security.BiometricAuth
import com.aurax.imagery.security.LockPolicy
import com.aurax.imagery.security.LockScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurax.imagery.ui.AuraXRoot
import com.aurax.imagery.ui.theme.AuraXTheme

class MainActivity : ComponentActivity() {
    private var lockEnabled = false
    private var unlocked = false
    private var backgroundedAt: Long? = null
    private var locked by mutableStateOf(false)
    private var authSignal: CancellationSignal? = null

    private fun authenticate() {
        if (authSignal != null) return
        authSignal = BiometricAuth.prompt(this, "Unlock AuraX") { ok ->
            authSignal = null
            if (ok) {
                unlocked = true
                backgroundedAt = null
                locked = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (LockPolicy.shouldLock(lockEnabled, unlocked, backgroundedAt, SystemClock.elapsedRealtime())) {
            unlocked = false
            locked = true
            authenticate()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
        authSignal?.cancel()
        authSignal = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent system bars so the AMOLED background and glass bar run edge to edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Read the setting before the first frame so a locked app never flashes the library.
        if (savedInstanceState == null) {
            val on = runBlocking { (application as AuraXApp).container.preferences.appLock.first() }
            lockEnabled = on
            if (on) {
                locked = true
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                authenticate()
            }
        }
        val glassSettings = (application as AuraXApp).container.glassSettings
        setContent {
            val glassStyle by glassSettings.style.collectAsStateWithLifecycle()
            AuraXTheme(glassStyle = glassStyle) {
                if (locked) LockScreen(onUnlock = ::authenticate) else AuraXRoot()
            }
        }
        // Follow later changes to the setting.
        lifecycleScope.launch {
            (application as AuraXApp).container.preferences.appLock.collect { on ->
                lockEnabled = on
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (on && !unlocked && !locked) {
                    locked = true
                    authenticate()
                }
                if (!on) locked = false
            }
        }
        // Refresh the index on a fresh launch (not on rotation). The worker itself checks the
        // media permission, so this is safe before the user has granted anything.
        if (savedInstanceState == null) {
            (application as AuraXApp).container.indexer.enqueue()
        }
    }
}
