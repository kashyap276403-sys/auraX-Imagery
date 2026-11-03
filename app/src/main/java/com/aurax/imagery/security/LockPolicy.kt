package com.aurax.imagery.security

/** When the app should ask for authentication again. Pure, so it is unit-tested. */
object LockPolicy {
    /** Leaving the app for less than this (answering a notification, say) does not re-lock it. */
    const val GRACE_MS = 15_000L

    /**
     * @param enabled the App Lock setting
     * @param unlocked whether the user already authenticated in this run
     * @param backgroundedAt uptime when the app last stopped, or null if it has not
     */
    fun shouldLock(enabled: Boolean, unlocked: Boolean, backgroundedAt: Long?, now: Long): Boolean {
        if (!enabled) return false
        if (!unlocked) return true
        return backgroundedAt != null && now - backgroundedAt >= GRACE_MS
    }
}
