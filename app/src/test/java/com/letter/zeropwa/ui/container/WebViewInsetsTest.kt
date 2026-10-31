package com.letter.zeropwa.ui.container

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewInsetsTest {
    @Test
    fun immersiveViewportDoesNotSubtractStatusBar() {
        assertEquals(ViewportOffsets(0, 0, 0, 0), viewportOffsets(1080, 2400, 0, 0, 1080, 2400))
    }

    @Test
    fun nativeStatusProtectionOnlySubtractsTop() {
        assertEquals(ViewportOffsets(0, 84, 0, 0), viewportOffsets(1080, 2400, 0, 84, 1080, 2316))
    }

    @Test
    fun imeResizeDoesNotSubtractTopSafeArea() {
        assertEquals(ViewportOffsets(0, 0, 0, 900), viewportOffsets(1080, 2400, 0, 0, 1080, 1500))
    }

    @Test
    fun nativeToolbarAndImeAreNotAppliedTwice() {
        assertEquals(ViewportOffsets(0, 252, 0, 900), viewportOffsets(1080, 2400, 0, 252, 1080, 1248))
    }

    @Test
    fun landscapeCutoutAndViewportOffsetsArePerEdge() {
        assertEquals(ViewportOffsets(90, 0, 120, 400), viewportOffsets(2400, 1080, 90, 0, 2190, 680))
    }

    @Test
    fun animationOvershootNeverCreatesNegativeOffsets() {
        assertEquals(ViewportOffsets(0, 0, 0, 0), viewportOffsets(1080, 2400, -10, -10, 1100, 2420))
    }
}
