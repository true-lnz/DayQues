package com.gfg.article.customloadingbutton

import android.animation.AnimatorInflater
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import ru.lansonz.dayquestion.R
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bgColor: Int
    private var textColor: Int
    private var progressColor: Int
    private var textSize: Float
    private var cornerRadius: Float
    private var progressMax: Int

    @Volatile
    private var progress: Double = 0.0
    private var valueAnimator: ValueAnimator

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _, _ ->
    }

    private val updateListener = ValueAnimator.AnimatorUpdateListener {
        progress = (it.animatedValue as Float).toDouble()
        invalidate()
        requestLayout()
    }

    fun hasCompletedDownload() {
        valueAnimator.cancel()
        buttonState = ButtonState.Completed
        invalidate()
        requestLayout()
    }

    private lateinit var paint: Paint

    init {
        isClickable = true
        valueAnimator = AnimatorInflater.loadAnimator(
            context, R.animator.loading_animation
        ) as ValueAnimator

        valueAnimator.addUpdateListener(updateListener)

        val attr = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingButton,
            0,
            0
        )
        try {
            bgColor = attr.getColor(
                R.styleable.LoadingButton_bgColor,
                ContextCompat.getColor(context, R.color.colorPrimary)
            )
            textColor = attr.getColor(
                R.styleable.LoadingButton_textColor,
                ContextCompat.getColor(context, R.color.whiteTextColor)
            )
            progressColor = attr.getColor(
                R.styleable.LoadingButton_progressColor,
                Color.parseColor("#004349")
            )
            textSize = attr.getDimension(
                R.styleable.LoadingButton_textSize,
                55.0f
            )
            cornerRadius = attr.getDimension(
                R.styleable.LoadingButton_cornerRadius,
                12f
            )
            progressMax = attr.getInt(
                R.styleable.LoadingButton_progressMax,
                100
            )
        } finally {
            attr.recycle()
        }

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            this.textSize = this@LoadingButton.textSize
            typeface = Typeface.create("", Typeface.BOLD)
        }

        setLayerType(LAYER_TYPE_SOFTWARE, paint)
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (buttonState == ButtonState.Completed) buttonState = ButtonState.Loading
        animation()

        return true
    }

    private fun animation() {
        valueAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.strokeWidth = 0f
        paint.color = bgColor

        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

        if (buttonState == ButtonState.Loading) {
            paint.color = progressColor
            canvas.drawRoundRect(
                0f, 0f,
                (width * (progress / progressMax)).toFloat(), height.toFloat(),
                cornerRadius, cornerRadius, paint
            )
        }

        val buttonText = if (buttonState == ButtonState.Loading)
            resources.getString(R.string.auth)
        else resources.getString(R.string.cancel)

        paint.color = textColor
        canvas.drawText(buttonText, (width / 2).toFloat(), ((height + 30) / 2).toFloat(), paint)
    }
}
