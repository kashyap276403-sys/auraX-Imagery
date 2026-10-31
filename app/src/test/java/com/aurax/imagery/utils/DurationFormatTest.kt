package com.aurax.imagery.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test fun zero() = assertEquals("0:00", formatDuration(0))

    @Test fun secondsArePadded() = assertEquals("0:05", formatDuration(5_000))

    @Test fun minutesAndSeconds() = assertEquals("1:05", formatDuration(65_000))

    @Test fun subSecondRoundsDown() = assertEquals("0:01", formatDuration(1_999))

    @Test fun hoursSwitchFormat() = assertEquals("1:01:01", formatDuration(3_661_000))

    @Test fun negativeClampsToZero() = assertEquals("0:00", formatDuration(-4_000))
}
