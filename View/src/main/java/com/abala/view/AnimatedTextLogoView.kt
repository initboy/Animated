package com.abala.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.core.util.set
import java.lang.IllegalArgumentException
import kotlin.math.abs

class AnimatedTextLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : View(context, attrs, style) {
    companion object {
        const val TAG = "AnimatedTextLogoView"
        const val DEFAULT_TEXT_COLOR = Color.BLACK
        const val DEFAULT_TEXT_SIZE = 24F
        const val DEFAULT_TEXT_LOGO = "Abala Note"
        const val DEFAULT_DURATION_TRANSLATE = 1236L
        const val DEFAULT_DURATION_GRADIENT = 764L
        const val OPAQUE = 255
        const val GOLDEN_SECTION = 0.618122977F
        const val PERCENT_50 = 0.5F
        const val FLOAT_OFFSET = 0.5F
    }

    private var mTextLogo: String = DEFAULT_TEXT_LOGO
    private var mWordSpacing = 0F
    private var mWordOffset = 0F
    private var mWidth = 0
    private var mTextColor = 0
    private var mTextSize = 0F
    private var mGradientColor = Color.WHITE
    private var mHeight = 0
    private var mTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mTextWidth = 0F
    private var mTextHeight = 0F
    private var mStartTextY = 0F
    private var mCenterX = 0
    private var mCenterY = 0

    private var mTranslateAnimation: ValueAnimator? = null
    private var mTranslateEnd = false
    private var mTranslateProgress = 0F

    private var mGradientAnimation: ValueAnimator? = null
    private var mLinearGradient: LinearGradient? = null
    private var mGradientTranslate: Int = 0
    private val mGradientMatrix = Matrix()
    private var mGradientEnd = true

    private val mCharRandomPoint = SparseArray<PointF>()
    private val mCharPoint = SparseArray<PointF>()

    //阴阳鱼
    private var mTaijiRadius = 0F
    private var mTaijiCenterX = 0F
    private var mTaijiCenterY = 0F
    private val mBlackFishPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWhiteFishPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.AnimatedTextLogoView)
        mTextColor = arr.getColor(R.styleable.AnimatedTextLogoView_text_color, DEFAULT_TEXT_COLOR)
        mTextSize = arr.getDimension(R.styleable.AnimatedTextLogoView_text_size, DEFAULT_TEXT_SIZE)
        mWordSpacing = arr.getDimension(R.styleable.AnimatedTextLogoView_word_spacing, 0F)
        mTextLogo = arr.getString(R.styleable.AnimatedTextLogoView_text_logo) ?: DEFAULT_TEXT_LOGO
        configTextPaint()
        arr.recycle()
    }

    private fun configTextPaint() {
        logi("configTextPaint")
        mTextPaint.color = mTextColor
        mTextPaint.textSize = mTextSize
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onAttachedToWindow() {
        logi("onAttachedToWindow")
        super.onAttachedToWindow()
        if (visibility == VISIBLE) {
            mTranslateAnimation?.start()
        }
    }

    override fun onDetachedFromWindow() {
        logi("onDetachedFromWindow")
        if (mTranslateAnimation?.isRunning == true) mTranslateAnimation?.cancel()
        if (mGradientAnimation?.isRunning == true) mGradientAnimation?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        logi("onSizeChanged")
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        createCharCoordinate()
        createTranslateAnimator()
        createGradientAnimator()
    }


    override fun onDraw(canvas: Canvas?) {
        canvas?.drawColor(0xFFE0E0E0.toInt())
        if ((mCenterX == 0) || (mCenterY == 0)) {
            initTaiji()
        }
        if (mTranslateEnd) {
            drawGradient(canvas)
        } else {
            drawTranslate(canvas)
        }
        drawTaiji(canvas)
    }

    private fun drawTaiji(canvas: Canvas?) {
        //移动到太极心
        canvas?.translate(mTaijiCenterX, mTaijiCenterY)
        //旋转
        canvas?.rotate(mTranslateProgress * 360)

        mBlackFishPaint.color = Color.BLACK
        mWhiteFishPaint.color = Color.WHITE
        //鱼身
        val taijiRect = RectF(-mTaijiRadius, -mTaijiRadius, mTaijiRadius, mTaijiRadius)
        canvas?.drawArc(taijiRect, 90F, 180F, true, mBlackFishPaint)
        canvas?.drawArc(taijiRect, -90F, 180F, true, mWhiteFishPaint)

        //鱼头
        val headRadius = mTaijiRadius / 2F
        canvas?.drawCircle(0F, -headRadius, headRadius, mBlackFishPaint)
        canvas?.drawCircle(0F, headRadius, headRadius, mWhiteFishPaint)

        //鱼眼
        val eyeRadius = headRadius * (1F - GOLDEN_SECTION)
        canvas?.drawCircle(0F, -headRadius, eyeRadius, mWhiteFishPaint)
        canvas?.drawCircle(0F, headRadius, eyeRadius, mBlackFishPaint)
    }

    private fun drawGradient(canvas: Canvas?) {
        mCharPoint.forEach { i, p ->
            canvas?.drawText(mTextLogo[i].toString(), p.x, p.y, mTextPaint)
        }
        mGradientMatrix.setTranslate(mGradientTranslate.toFloat(), 0F)
        mLinearGradient?.setLocalMatrix(mGradientMatrix)
    }

    private fun drawTranslate(canvas: Canvas?) {
        if (mTranslateAnimation?.isRunning == false) {
            start()
        }
        mTextPaint.alpha = OPAQUE.coerceAtMost((OPAQUE * mTranslateProgress + FLOAT_OFFSET).toInt())
        mCharPoint.forEach { i, p ->
            val rp = mCharRandomPoint[i]
            val x = rp.x + (p.x - rp.x) * mTranslateProgress
            val y = rp.y + (p.y - rp.y) * mTranslateProgress
            canvas?.drawText(mTextLogo[i].toString(), x, y, mTextPaint)
        }
    }

    private fun start() {
        logi("start")
        if (mGradientEnd) {
            mTranslateAnimation?.start()
            mGradientEnd = false
        }
    }

    private fun createCharCoordinate() {
        logi("initCharCoordinate")
        if ((mWidth == 0) || (mHeight == 0)) {
            Log.w(TAG, "the width and height not measure, initCharCoordinate later.")
            return
        }
        mTextWidth = 0F
        //计算文本宽度
        mTextLogo.forEachIndexed { i: Int, c: Char ->
            val charLength = mTextPaint.measureText(c.toString())
            mTextWidth += if (i < mTextLogo.length - 1) {
                charLength + mWordOffset
            } else {
                charLength
            }
        }
        //计算文本高度
        val font: Paint.FontMetrics? = mTextPaint.fontMetrics
        mTextHeight = (font?.descent ?: 0F) - (font?.ascent ?: 0F)

        if (mTextWidth > mWidth) {
            throw IllegalArgumentException("the text logo length:$mTextWidth must < screen width:$mWidth.")
        }
        mCharPoint.clear()
        var startTextX = (mWidth - mTextWidth) / 2F
        mStartTextY = mHeight / 2F + mTextHeight / 2F
        mTextLogo.forEachIndexed { i: Int, c: Char ->
            val charLength = mTextPaint.measureText(c.toString())
            mCharPoint[i] = PointF(startTextX, mStartTextY)
            startTextX += charLength + mWordOffset
        }

        mCharRandomPoint.clear()
        mTextLogo.forEachIndexed { i: Int, _: Char ->
            val x = (0..mWidth).random().toFloat()
            val y = (0..mHeight).random().toFloat()
            mCharRandomPoint[i] = PointF(x, y)
        }
        logi("mTextWidth = $mTextWidth mTextHeight = $mTextHeight  mWidth = $mWidth mHeight = $mHeight")
    }

    private fun initTaiji() {
        mCenterX = width / 2
        mCenterY = height / 2

        val taijiHeight = mTextWidth * GOLDEN_SECTION
        val taijiSpace = abs(taijiHeight / GOLDEN_SECTION - mTextHeight - taijiHeight)
        mTaijiRadius = taijiHeight / 2F
        mTaijiCenterX = mCenterX.toFloat()
        mTaijiCenterY = mStartTextY - mTaijiRadius - mTextHeight - taijiSpace
        logi("taijiRadius = $mTaijiRadius taijiSpace = $taijiSpace  mTaijiCenterY = $mTaijiCenterY mTaijiCenterX = $mTaijiCenterX")
    }

    private fun createTranslateAnimator() {
        logi("createTranslateAnimator")
        if (mTranslateAnimation == null) {
            mTranslateAnimation = ValueAnimator.ofFloat(0F, 1F)
            mTranslateAnimation?.interpolator = AccelerateInterpolator()
            mTranslateAnimation?.duration = DEFAULT_DURATION_TRANSLATE
            mTranslateAnimation?.addUpdateListener { anim ->
                if (mCharPoint.isEmpty() || mCharRandomPoint.isEmpty()) {
                    return@addUpdateListener
                }
                mTranslateProgress = anim?.animatedValue as? Float ?: 0F
                invalidate()
            }
            mTranslateAnimation?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    mTranslateEnd = true
                    mLinearGradient?.let {
                        mTextPaint.shader = it
                    }
                    mGradientAnimation?.start()
                }
            })
        }
    }

    private fun createGradientAnimator() {
        logi("createGradientAnimator")
        if ((mWidth == 0) || (mHeight == 0)) {
            Log.w(TAG, "the width and height not measure, createGradientAnimator later.")
            return
        }
        if (mGradientAnimation == null) {
            mGradientAnimation = ValueAnimator.ofInt(0, width * 2)
            mGradientAnimation?.duration = DEFAULT_DURATION_GRADIENT
            mGradientAnimation?.addUpdateListener { anim ->
                mGradientTranslate = anim?.animatedValue as? Int ?: 0
                invalidate()
            }
            mGradientAnimation?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    mGradientEnd = true
                }
            })
            val colors = intArrayOf(mTextColor, mGradientColor, mTextColor)
            val positions = floatArrayOf(0F, 0.5F, 1F)
            mLinearGradient = LinearGradient(-width.toFloat(), 0F, 0F, 0F, colors, positions, Shader.TileMode.CLAMP)
        }
    }

}

inline fun <reified T> T.logi(info: String) {
    Log.i(T::class.java.simpleName, info)
}