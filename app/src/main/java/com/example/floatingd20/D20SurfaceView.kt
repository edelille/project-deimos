package com.example.floatingd20

import android.content.Context
import android.util.AttributeSet
import org.rajawali3d.view.SurfaceView

class D20SurfaceView : SurfaceView {
    private var renderer: D20Renderer? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        renderer = D20Renderer(context)
        setFrameRate(60.0)
        setSurfaceRenderer(renderer)
    }

    fun startSequentialRoll() {
        renderer?.startSequentialRoll()
    }

    fun rollWithDuration(durationSeconds: Float = 5.0f) {
        renderer?.rollWithDuration(durationSeconds)
    }

    fun rotateByFinger(dx: Float, dy: Float, isNewTouch: Boolean = false, isEndTouch: Boolean = false) {
        renderer?.rotateByFinger(dx, dy, isNewTouch, isEndTouch)
    }

    fun getCurrentTopFace(): Int {
        return renderer?.getCurrentTopFace() ?: 0
    }

    fun getCurrentEulerAngles(): FloatArray {
        return renderer?.getCurrentEulerAngles() ?: FloatArray(3)
    }
} 