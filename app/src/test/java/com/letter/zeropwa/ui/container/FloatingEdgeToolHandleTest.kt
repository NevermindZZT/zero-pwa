package com.letter.zeropwa.ui.container

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingEdgeToolHandleTest {
    @Test fun handleFitsNormalViewport() {
        assertEquals(566.4f, handleVerticalOffset(1000f, 56f, 0.6f), 0.01f)
    }

    @Test fun handleRemainsVisibleWhenKeyboardOrLandscapeShrinksViewport() {
        assertEquals(86.4f, handleVerticalOffset(200f, 56f, 0.6f), 0.01f)
        assertEquals(0f, handleVerticalOffset(40f, 56f, 0.6f), 0.01f)
    }

    @Test fun outOfRangeSavedOrDraggedPositionsAreClamped() {
        assertEquals(0f, handleVerticalOffset(1000f, 56f, -1f), 0.01f)
        assertEquals(944f, handleVerticalOffset(1000f, 56f, 2f), 0.01f)
    }
}
