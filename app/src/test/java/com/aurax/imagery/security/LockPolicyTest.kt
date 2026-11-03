package com.aurax.imagery.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    @Test fun offNeverLocks() = assertFalse(LockPolicy.shouldLock(false, false, null, 0))
    @Test fun coldStartLocks() = assertTrue(LockPolicy.shouldLock(true, false, null, 0))
    @Test fun briefAbsenceKeepsUnlocked() = assertFalse(LockPolicy.shouldLock(true, true, 1_000, 1_000 + LockPolicy.GRACE_MS - 1))
    @Test fun longAbsenceRelocks() = assertTrue(LockPolicy.shouldLock(true, true, 1_000, 1_000 + LockPolicy.GRACE_MS))
    @Test fun neverBackgroundedStaysUnlocked() = assertFalse(LockPolicy.shouldLock(true, true, null, 99_999))
}
