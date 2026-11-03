package com.aurax.imagery.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Opaque black cover shown instead of the app until authentication succeeds. Shows nothing of the library. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onUnlock),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.padding(bottom = 16.dp))
        Text("AuraX is locked", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onUnlock, modifier = Modifier.padding(top = 20.dp)) { Text("Unlock") }
    }
}
