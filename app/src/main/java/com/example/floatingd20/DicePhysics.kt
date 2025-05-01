package com.example.floatingd20

import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DicePhysics {
    private var angularVelocityX = 0f
    private var angularVelocityY = 0f
    private var angularVelocityZ = 0f
    private var rotationX = 0f
    private var rotationY = 0f
    private var rotationZ = 0f
    private var isRolling = false
    private var rollTime = 0f
    private val FRICTION = 0.98f
    private val GRAVITY = 9.8f
    private val MAX_ROLL_TIME = 3.0f

    fun startRoll(velocityX: Float, velocityY: Float) {
        angularVelocityX = velocityX * 0.1f
        angularVelocityY = velocityY * 0.1f
        angularVelocityZ = (velocityX + velocityY) * 0.05f
        isRolling = true
        rollTime = 0f
    }

    fun update(deltaTime: Float): FloatArray {
        if (!isRolling) return floatArrayOf(rotationX, rotationY, rotationZ)

        rollTime += deltaTime
        if (rollTime >= MAX_ROLL_TIME) {
            isRolling = false
            return floatArrayOf(rotationX, rotationY, rotationZ)
        }

        // Apply friction
        angularVelocityX *= FRICTION
        angularVelocityY *= FRICTION
        angularVelocityZ *= FRICTION

        // Update rotation
        rotationX += angularVelocityX * deltaTime
        rotationY += angularVelocityY * deltaTime
        rotationZ += angularVelocityZ * deltaTime

        // Normalize rotations
        rotationX %= 360f
        rotationY %= 360f
        rotationZ %= 360f

        // Check if dice has settled
        val speed = sqrt(
            angularVelocityX * angularVelocityX +
            angularVelocityY * angularVelocityY +
            angularVelocityZ * angularVelocityZ
        )
        
        if (speed < 0.1f) {
            isRolling = false
        }

        return floatArrayOf(rotationX, rotationY, rotationZ)
    }

    fun getCurrentFace(): Int {
        // Convert rotations to face number (1-20)
        val normalizedX = (rotationX % 360f + 360f) % 360f
        val normalizedY = (rotationY % 360f + 360f) % 360f
        val normalizedZ = (rotationZ % 360f + 360f) % 360f

        // This is a simplified version - in reality, you'd need to calculate
        // which face is pointing up based on the actual geometry
        val face = when {
            normalizedY < 30f -> 20
            normalizedY < 90f -> 1
            normalizedY < 150f -> 2
            normalizedY < 210f -> 3
            normalizedY < 270f -> 4
            normalizedY < 330f -> 5
            else -> 6
        }

        return face
    }

    fun isRolling(): Boolean = isRolling
} 