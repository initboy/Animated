package com.abala.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * 自定义ViewGroup
 * 1、布局：onLayout,onMeasure
 * 2、显示:onDraw->canvas,Paint,Path,Rect,Line,Arc,Circle,Text,Matrix,Clip,Animation
 * 3、交互：onTouchEvent 事件分发
 */
/**
 * 绘制流程
 * 开始
 * 构造函数
 * onMeasure()
 * onSizeChanged()
 * onLayout()
 * onDraw()<-invalidate(),用户操作或自身状态发生改变
 * 结束
 */
//构造一个参数是在代码中实例化的，两个参数是在xml布局文件中使用，第三个参数定义主题，第四个参数定义属性
class FlowContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ViewGroup(context, attrs, defStyleAttr) {

    //按行存储每行的views
    private val mAllLines = ArrayList<List<View>>()
    private val mLineHeights = ArrayList<Int>()
    private val mHorizontalSpacing = dp2px(16F)
    private val mVerticalSpacing = dp2px(8F)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //得到父容器分配的宽高
        val dispatchWidth = MeasureSpec.getSize(widthMeasureSpec)
        val dispatchHeight = MeasureSpec.getSize(heightMeasureSpec)

        //onMeasure可能会被多次调用，需要清理数据
        mAllLines.clear()
        mLineHeights.clear()
        var lineViews = arrayListOf<View>()
        //记录一行已经使用的宽高
        var lineWidth = 0
        var lineHeight = 0

        //计算父容器需要的宽高
        var parentHeight = 0
        var parentWidth = 0

        for (i in 0 until childCount) {
            val childView: View? = getChildAt(i)
            childView?.apply {
                if (View.GONE != this@apply.visibility) {
                    //得到子View的MeasureSpec
                    val childWidthMeasureSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        this@FlowContainer.paddingStart + this@FlowContainer.paddingEnd,
                        this@apply.layoutParams.width
                    )
                    val childHeightMeasureSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        this@FlowContainer.paddingTop + this@FlowContainer.paddingBottom,
                        this@apply.layoutParams.height
                    )
                    //测量子View
                    measure(childWidthMeasureSpec, childHeightMeasureSpec)

                    //换行
                    val isNeedWrap = this@apply.measuredWidth + lineWidth + mHorizontalSpacing > dispatchWidth
                    logi("isNeedWrap = $isNeedWrap")
                    if (isNeedWrap) {
                        mAllLines.add(lineViews)
                        mLineHeights.add(lineHeight)
                        parentWidth = parentWidth.coerceAtLeast(lineWidth + mHorizontalSpacing)
                        parentHeight += lineHeight + mVerticalSpacing

                        //需要新的容器存储一行View
                        lineViews = arrayListOf<View>()
                        lineHeight = 0
                        lineWidth = 0
                    }

                    //记录每行的view及宽高，后面用来onLayout
                    lineViews.add(this@apply)
                    lineWidth += this@apply.measuredWidth + mHorizontalSpacing
                    lineHeight = lineHeight.coerceAtLeast(this@apply.measuredHeight)

                    //最后一行的逻辑
                    if (i == childCount - 1) {
                        mAllLines.add(lineViews)
                        mLineHeights.add(lineHeight)
                        parentHeight += lineHeight + mVerticalSpacing
                        parentWidth = parentWidth.coerceAtLeast(lineWidth + mHorizontalSpacing)
                    }
                }
            }
        }

        //测量自己并保存
        val selfWidth = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            dispatchWidth
        } else {
            parentWidth
        }
        val selfHeight = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            dispatchHeight
        } else {
            parentHeight
        }
        setMeasuredDimension(selfWidth, selfHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        //记录每个view布局时的起点
        var cursorLeft = this@FlowContainer.paddingLeft
        var cursorTop = this@FlowContainer.paddingTop
        mAllLines.forEachIndexed { index, lineViews ->
            val lineHeight = mLineHeights[index]
            lineViews.forEach {
                val left = cursorLeft
                val top = cursorTop
                val right = left + it.measuredWidth
                val bottom = top + it.measuredHeight
                it.layout(left, top, right, bottom)
                //更新X坐标
                cursorLeft = right + mHorizontalSpacing
            }
            //换行后更新起点
            cursorTop += lineHeight + mVerticalSpacing
            cursorLeft = this@FlowContainer.paddingLeft
        }

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    private fun dp2px(dp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        Resources.getSystem().displayMetrics
    ).toInt()

}