package com.letter.zeropwa.ui.container

import android.view.View
import android.webkit.WebView
import androidx.core.view.ViewCompat

/** Space already occupied outside the WebView, in window pixels (not dp/CSS pixels). */
internal data class ViewportOffsets(val left: Int, val top: Int, val right: Int, val bottom: Int)

internal fun viewportOffsets(
    windowWidth: Int,
    windowHeight: Int,
    left: Int,
    top: Int,
    width: Int,
    height: Int
): ViewportOffsets = ViewportOffsets(
    left.coerceAtLeast(0),
    top.coerceAtLeast(0),
    (windowWidth - left - width).coerceAtLeast(0),
    (windowHeight - top - height).coerceAtLeast(0)
)

/**
 * Compose 1.6 AndroidView does not subtract the space already reserved by its layout.
 * Preserve real status/cutout insets in immersive mode, but do not pass the IME/top
 * toolbar space a second time when Compose has already resized/offset the WebView.
 * Never change the document's CSS or assume a particular site's class names.
 */
internal fun WebView.configureViewportInsets() {
    val location = IntArray(2)
    val rootLocation = IntArray(2)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        if (view.width == 0 || view.height == 0) {
            ViewCompat.onApplyWindowInsets(view, insets)
        } else {
            val root = view.rootView
            view.getLocationInWindow(location)
            root.getLocationInWindow(rootLocation)
            val offsets = viewportOffsets(
                root.width, root.height,
                location[0] - rootLocation[0], location[1] - rootLocation[1],
                view.width, view.height
            )
            ViewCompat.onApplyWindowInsets(
                view, insets.inset(offsets.left, offsets.top, offsets.right, offsets.bottom)
            )
        }
    }
    // IME animation, rotation, status-bar protection and native toolbar change geometry.
    addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            ViewCompat.requestApplyInsets(view)
        }
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = ViewCompat.requestApplyInsets(view)
        override fun onViewDetachedFromWindow(view: View) = Unit
    })
}
