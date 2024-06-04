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
    var progressColor: Int
    private var textSize: Float
    private var cornerRadius: Float
    private var progressMax: Int
    private var buttonTextValue: String
    private var rightTextValue: String
    private var rightTextColorValue: Int
    private var progressPercentage: Float

    private var valueAnimator: ValueAnimator

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _, _ ->
        invalidate()
        requestLayout()
    }

    private val updateListener = ValueAnimator.AnimatorUpdateListener {
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
            buttonTextValue = attr.getString(
                R.styleable.LoadingButton_buttonText
            ) ?: context.getString(R.string.error)
            rightTextValue = attr.getString(
                R.styleable.LoadingButton_rightText
            ) ?: ""
            rightTextColorValue = attr.getColor(
                R.styleable.LoadingButton_rightTextColor,
                ContextCompat.getColor(context, R.color.whiteTextColor)
            )
            progressPercentage = attr.getFloat(
                R.styleable.LoadingButton_progressPercentage,
                1.0f // Default value is 1.0 (100%)
            )
        } finally {
            attr.recycle()
        }

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            this.textSize = this@LoadingButton.textSize
            typeface = Typeface.create("", Typeface.NORMAL)
        }

        setLayerType(LAYER_TYPE_SOFTWARE, paint)
    }

    var buttonText: String
        get() = buttonTextValue
        set(value) {
            buttonTextValue = value
            invalidate()
            requestLayout()
        }

    var rightText: String
        get() = rightTextValue
        set(value) {
            rightTextValue = value
            invalidate()
            requestLayout()
        }

    var rightTextColor: Int
        get() = rightTextColorValue
        set(value) {
            rightTextColorValue = value
            invalidate()
            requestLayout()
        }

    var progressPercentageValue: Float
        get() = progressPercentage
        set(value) {
            progressPercentage = value
            invalidate()
            requestLayout()
        }

    override fun performClick(): Boolean {
        super.performClick()
        if (buttonState == ButtonState.Completed) startLoading()

        return true
    }

    private fun startLoading() {
        buttonState = ButtonState.Loading
        valueAnimator.setFloatValues(0f, progressPercentage)
        valueAnimator.start()
    }

    fun startAnimation() {
        if (buttonState != ButtonState.Loading) {
            startLoading()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.strokeWidth = 0f
        paint.color = bgColor

        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val verticalTextOffset = (height - textHeight) / 2 - fontMetrics.ascent

        if (buttonState == ButtonState.Loading) {
            paint.color = progressColor
            val progressFraction = (valueAnimator.animatedValue as Float).coerceAtMost(progressPercentage)
            val progressWidth = width * progressFraction

            // Сохранить состояние канвы
            canvas.save()

            // Ограничить область рисования полосы загрузки
            canvas.clipRect(0f, 0f, progressWidth, height.toFloat())

            // Нарисовать полосу загрузки
            canvas.drawRoundRect(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                cornerRadius, cornerRadius, paint
            )

            // Восстановить состояние канвы
            canvas.restore()
        }

        // Нарисовать текст слева
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(buttonTextValue, 30f, verticalTextOffset, paint)

        // Нарисовать текст справа при загрузке
        if (buttonState == ButtonState.Loading) {
            paint.color = rightTextColorValue
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(rightTextValue, (width - 30).toFloat(), verticalTextOffset, paint)
        }
    }

}
