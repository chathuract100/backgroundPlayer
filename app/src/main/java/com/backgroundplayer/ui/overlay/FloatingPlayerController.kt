package com.backgroundplayer.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.ImageLoader
import coil.request.ImageRequest
import com.backgroundplayer.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A draggable "floating tile" mini-player drawn over other apps, shown for audio
 * when the app is backgrounded (video uses Picture-in-Picture instead). Tapping
 * it reopens the app; it auto-dismisses when playback ends. The window mechanics
 * are device-verified; the decision to show lives in [com.backgroundplayer.playback.OverlayPolicy].
 */
class FloatingPlayerController(
    private val context: Context,
    private val playback: PlaybackController,
    private val onTapReopen: () -> Unit,
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var root: View? = null
    private var titleView: TextView? = null
    private var artView: ImageView? = null
    private var playPauseView: ImageButton? = null
    private var scope: CoroutineScope? = null

    val isShowing: Boolean get() = root != null

    fun show() {
        if (isShowing) return

        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#E61A1626"))
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }
        }

        val art = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#8B5CF6"))
            }
            clipToOutline = true
        }
        val title = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(120), LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(10); marginEnd = dp(6) }
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val playPause = iconButton(android.R.drawable.ic_media_pause, dp(40)) {
            if (playback.state.value.isPlaying) playback.pause() else playback.resume()
        }
        val next = iconButton(android.R.drawable.ic_media_next, dp(40)) { playback.next() }
        val close = iconButton(android.R.drawable.ic_menu_close_clear_cancel, dp(36)) { hide() }

        container.addView(art)
        container.addView(title)
        container.addView(playPause)
        container.addView(next)
        container.addView(close)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(96)
        }

        attachDragAndTap(container, params)

        windowManager.addView(container, params)
        root = container
        titleView = title
        artView = art
        playPauseView = playPause

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        newScope.launch {
            playback.state.collectLatest { state ->
                val track = state.currentTrack
                if (track == null) {
                    hide()
                    return@collectLatest
                }
                title.text = track.title
                playPause.setImageResource(
                    if (state.isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                )
                loadArtwork(art, track.thumbnailUri)
            }
        }
    }

    fun hide() {
        scope?.cancel()
        scope = null
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        titleView = null
        artView = null
        playPauseView = null
    }

    private fun iconButton(resId: Int, sizePx: Int, onClick: () -> Unit): ImageButton =
        ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
            setImageResource(resId)
            setColorFilter(Color.WHITE)
            background = null
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener { onClick() }
        }

    private suspend fun loadArtwork(view: ImageView, uri: String?) {
        if (uri == null) {
            view.setImageDrawable(null)
            return
        }
        val request = ImageRequest.Builder(context).data(uri).size(96).build()
        val drawable = ImageLoader(context).execute(request).drawable
        if (drawable is BitmapDrawable) view.setImageDrawable(drawable)
    }

    private fun attachDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var dragged = false
        val touchSlop = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics,
        )

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) dragged = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) {
                        onTapReopen()
                        hide()
                    }
                    true
                }
                else -> false
            }
        }
        // Let child buttons still receive their own clicks: the touch listener is
        // on the container, buttons consume their taps before reaching it.
        if (view is FrameLayout) Unit
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
}
