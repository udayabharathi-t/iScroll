package app.lapse.tracking

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class CounterChip(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    fun show(text: String) {
        val existing = view
        if (existing != null) {
            if (existing.text.toString() == text) return
            existing.text = styled(text)
            val layout = params ?: return
            try {
                windowManager.updateViewLayout(existing, layout)
            } catch (_: RuntimeException) {
                view = null
            }
            return
        }
        val chip = TextView(context).apply {
            this.text = styled(text)
            setTextColor(0xFFF4EFE6.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            val padH = dp(14)
            val padV = dp(8)
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xF2100E0C.toInt())
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight() + dp(8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            windowManager.addView(chip, layout)
            view = chip
            params = layout
        } catch (_: RuntimeException) {
            view = null
            params = null
        }
    }

    fun hide() {
        val chip = view ?: return
        view = null
        params = null
        try {
            windowManager.removeView(chip)
        } catch (_: RuntimeException) {
            // Already detached.
        }
    }

    private fun styled(text: String): CharSequence {
        val split = text.indexOf(' ')
        if (split <= 0) return text
        return SpannableString(text).apply {
            setSpan(AbsoluteSizeSpan(20, true), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun statusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else dp(24)
    }
}
