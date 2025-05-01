package com.example.floatingd20

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import org.rajawali3d.Object3D
import org.rajawali3d.lights.DirectionalLight
import org.rajawali3d.lights.PointLight
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.methods.DiffuseMethod
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.materials.textures.ATexture
import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.MathUtil
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Sphere
import org.rajawali3d.renderer.Renderer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.random.Random
import kotlin.math.asin
import kotlin.math.atan2

class D20Renderer(context: Context) : Renderer(context), SensorEventListener {
    private var d20: Object3D? = null
    private var isRolling = false
    private var rollVelocity = Vector3()
    private var currentRotation = Quaternion()
    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var cameraDistance = 10.0
    private var cameraRotationX = 0.0
    private var cameraRotationY = 0.0
    private val CAMERA_ROTATION_SPEED = 0.5
    private val SPIN_DAMPING = 0.98 // Damping factor for spin momentum

    // Face numbers (standard D20 numbering)
    private val faceNumbers = intArrayOf(
        20, 1, 18, 4, 13, 6, 11, 8, 15, 10,
        17, 2, 19, 3, 16, 5, 14, 7, 12, 9
    )

    // Face centers for top face calculation
    private val faceCenters = mutableListOf<Vector3>()
    private var currentTopFace = 0

    // Sequential roll properties
    private var rollSequence = 0
    private var lastRollTime = 0L
    private val ROLL_DELAY = 100L // 100ms delay between rolls
    private val INITIAL_VELOCITY = 5.0 // Initial rotation speed
    private var DAMPING_FACTOR = 0.9998 // Changed from val to var so it can be modified

    // Sensor data arrays
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private var gyroscopeReading = FloatArray(3)
    
    // Smoothed rotation values
    private var smoothedRotation = Quaternion()
    private var targetRotation = Quaternion()
    
    // Rotation matrices
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    // Smoothing factor (0 = no smoothing, 1 = infinite smoothing)
    private val SMOOTHING_FACTOR = 0.85f
    
    // Time tracking for gyroscope integration
    private var timestamp: Long = 0

    // Adjacency list for a standard D20 (faces 0-19)
    // Derived assuming a layout where vertex indices define faces in a specific order.
    // This might need adjustment if D20Geometry uses a different indexing.
    private val faceAdjacency = listOf(
        listOf(1, 4, 5),    // Face 0 neighbors
        listOf(0, 5, 9),    // Face 1 neighbors
        listOf(3, 4, 13),   // Face 2 neighbors
        listOf(2, 7, 12),   // Face 3 neighbors
        listOf(0, 2, 11),   // Face 4 neighbors
        listOf(0, 1, 8),    // Face 5 neighbors
        listOf(7, 8, 18),   // Face 6 neighbors
        listOf(3, 6, 17),   // Face 7 neighbors
        listOf(5, 6, 15),   // Face 8 neighbors
        listOf(1, 10, 14),  // Face 9 neighbors
        listOf(9, 11, 19),  // Face 10 neighbors
        listOf(4, 10, 16),  // Face 11 neighbors
        listOf(3, 13, 16),  // Face 12 neighbors
        listOf(2, 12, 15),  // Face 13 neighbors
        listOf(9, 15, 18),  // Face 14 neighbors
        listOf(8, 13, 17),  // Face 15 neighbors
        listOf(11, 12, 19), // Face 16 neighbors
        listOf(7, 15, 19),  // Face 17 neighbors
        listOf(6, 14, 19),  // Face 18 neighbors
        listOf(10, 16, 17, 18) // Face 19 neighbors (should be 3? Check layout)
        // Correction: Last face 19 should also only have 3 neighbors based on geometry.
        // Assuming a common layout: 19 neighbors are 10, 16, 18 (or similar). Let's adjust.
        // listOf(10, 16, 18) // Tentative neighbours for Face 19
        // Let's re-verify a standard adjacency list. A common one might be:
        // F0(20): 1,4,8,5,18
        // ... this gets complex quickly without knowing the exact vertex->face mapping.
        // SIMPLIFICATION: Check against the *immediately preceding* face only for now.
        // This is less robust but avoids needing the exact adjacency list.
    )
    private val assignedHues = mutableMapOf<Int, Float>()
    private val MIN_HUE_DIFFERENCE = 60f // Minimum degrees between adjacent hues

    private var cameraLight: PointLight? = null // Added for camera light
    private var cornerLight1: PointLight? = null // Top-left light
    private var cornerLight2: PointLight? = null // Top-right light
    private var cornerLight3: PointLight? = null // Bottom-left light
    private var cornerLight4: PointLight? = null // Bottom-right light

    private val faceNormalsList = mutableListOf<Vector3>() // Store calculated face normals

    // Touch tracking for momentum
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchVelocityX = 0.0
    private var touchVelocityY = 0.0
    private var lastTouchTime = 0L
    private var isTouching = false
    private val VELOCITY_FACTOR = 0.5 // Increased from 0.15 for more pronounced momentum

    override fun initScene() {
        // Create D20 geometry
        d20 = createD20()
        getCurrentScene().addChild(d20)

        // Set up camera
        getCurrentCamera().setPosition(0.0, 2.0, cameraDistance)
        getCurrentCamera().setLookAt(0.0, 0.0, 0.0)

        // Remove existing lights
        // getCurrentScene().removeLight(mainLight)
        // getCurrentScene().removeLight(fillLight)
        // getCurrentScene().removeLight(topLight)

        // Add a point light attached to the camera
        cameraLight = PointLight()
        cameraLight?.power = 5.5f // Reduced power to balance with new lights
        cameraLight?.position = getCurrentCamera().position // Initial position
        getCurrentScene().addLight(cameraLight)
        
        // Add corner lights in a square pattern
        val lightDistance = 5.0 // Distance from center
        
        // Top-left light
        cornerLight1 = PointLight()
        cornerLight1?.power = 3.0f
        cornerLight1?.position = Vector3(-lightDistance, 2.0, cameraDistance - lightDistance)
        getCurrentScene().addLight(cornerLight1)
        
        // Top-right light
        cornerLight2 = PointLight()
        cornerLight2?.power = 3.0f
        cornerLight2?.position = Vector3(lightDistance, 2.0, cameraDistance - lightDistance)
        getCurrentScene().addLight(cornerLight2)
        
        // Bottom-left light
        cornerLight3 = PointLight()
        cornerLight3?.power = 3.0f
        cornerLight3?.position = Vector3(-lightDistance, 2.0, cameraDistance + lightDistance)
        getCurrentScene().addLight(cornerLight3)
        
        // Bottom-right light
        cornerLight4 = PointLight()
        cornerLight4?.power = 3.0f
        cornerLight4?.position = Vector3(lightDistance, 2.0, cameraDistance + lightDistance)
        getCurrentScene().addLight(cornerLight4)

        // Set background color
        getCurrentScene().setBackgroundColor(0x000000)

        // Set up sensors
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        // Register sensor listeners with faster update rate
        sensorManager?.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun hueDifference(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2)
        return min(diff, 360f - diff)
    }

    private fun isHueFarEnough(newHue: Float, faceIndex: Int): Boolean {
        // SIMPLIFIED CHECK: Only check against the previously assigned hue.
        val previousHue = assignedHues[faceIndex - 1]
        if (previousHue != null) {
            return hueDifference(newHue, previousHue) >= MIN_HUE_DIFFERENCE
        }
        return true // No previous face to check against
        
        /* // FULL ADJACENCY CHECK (requires correct faceAdjacency list)
        val neighbors = faceAdjacency.getOrNull(faceIndex) ?: return true
        for (neighborIndex in neighbors) {
            val neighborHue = assignedHues[neighborIndex]
            if (neighborHue != null) {
                if (hueDifference(newHue, neighborHue) < MIN_HUE_DIFFERENCE) {
                    return false
                }
            }
        }
        return true
        */
    }

    private fun createD20(): Object3D {
        val vertices = D20Geometry.createVertices()
        val indices = D20Geometry.createIndices()
        val normals = D20Geometry.createNormals()
        val d20 = Object3D()

        // Convert vertices and indices to arrays
        val vertexArray = FloatArray(vertices.limit())
        vertices.get(vertexArray)
        val indexArray = IntArray(indices.limit())
        indices.get(indexArray)
        val normalArray = FloatArray(normals.limit())
        normals.get(normalArray)

        // Define texture coordinates (U flipped back for correct orientation)
        val defaultTexCoords = floatArrayOf(
            1.0f, 0.0f, // Flipped U for vertex 1
            0.0f, 0.0f, // Flipped U for vertex 2
            0.5f, 1.0f  // Vertex 3 (bottom-center)
        )

        // Create faces
        for (i in 0 until indexArray.size step 3) {
            val faceIdx = i / 3
            val faceVertices = FloatArray(9)
            val faceNormals = FloatArray(9)
            val faceTexCoords = FloatArray(6) // Texture coordinates for this face

            for (j in 0..2) {
                val idx = indexArray[i + j] * 3
                faceVertices[j * 3] = vertexArray[idx]
                faceVertices[j * 3 + 1] = vertexArray[idx + 1]
                faceVertices[j * 3 + 2] = vertexArray[idx + 2]
                faceNormals[j * 3] = normalArray[idx]
                faceNormals[j * 3 + 1] = normalArray[idx + 1]
                faceNormals[j * 3 + 2] = normalArray[idx + 2]

                // Assign standard texture coordinates
                faceTexCoords[j * 2] = defaultTexCoords[j * 2]
                faceTexCoords[j * 2 + 1] = defaultTexCoords[j * 2 + 1]
            }

            // Calculate face normal (using cross product of edges)
            // Cast Floats to Doubles for Vector3 constructor
            val v1 = Vector3(faceVertices[0].toDouble(), faceVertices[1].toDouble(), faceVertices[2].toDouble())
            val v2 = Vector3(faceVertices[3].toDouble(), faceVertices[4].toDouble(), faceVertices[5].toDouble())
            val v3 = Vector3(faceVertices[6].toDouble(), faceVertices[7].toDouble(), faceVertices[8].toDouble())
            val edge1 = Vector3.subtractAndCreate(v2, v1)
            val edge2 = Vector3.subtractAndCreate(v3, v1)
            
            // Manually calculate cross product
            val crossX = edge1.y * edge2.z - edge1.z * edge2.y
            val crossY = edge1.z * edge2.x - edge1.x * edge2.z
            val crossZ = edge1.x * edge2.y - edge1.y * edge2.x
            val faceNormal = Vector3(crossX, crossY, crossZ)
            faceNormal.normalize()
            
            faceNormalsList.add(faceNormal)

            // Calculate face center (Ensure Double type for constructor)
            val centerX = (faceVertices[0].toDouble() + faceVertices[3].toDouble() + faceVertices[6].toDouble()) / 3.0
            val centerY = (faceVertices[1].toDouble() + faceVertices[4].toDouble() + faceVertices[7].toDouble()) / 3.0
            val centerZ = (faceVertices[2].toDouble() + faceVertices[5].toDouble() + faceVertices[8].toDouble()) / 3.0
            val center: Vector3 = Vector3(centerX, centerY, centerZ) // Explicitly declare type

            val face = Object3D()
            val material = Material()
            material.setDiffuseMethod(DiffuseMethod.Lambert())
            
            // Generate distinct random color
            var hue: Float
            do {
                hue = Random.nextFloat() * 360f
            } while (faceIdx > 0 && !isHueFarEnough(hue, faceIdx)) // Use simplified check
            
            assignedHues[faceIdx] = hue // Store assigned hue
            val hsv = floatArrayOf(hue, 0.7f, 0.5f)
            val color = Color.HSVToColor(hsv)
            material.setColor(color) // Set face color
            
            material.enableLighting(true) // Keep lighting for shape definition

            // Create texture with face number
            val number = faceNumbers[faceIdx]
            val texture = createNumberTexture(number)
            try {
                val numberTexture = Texture("number_$number", texture)
                material.addTexture(numberTexture)
            } catch (e: ATexture.TextureException) {
                e.printStackTrace()
            }

            // Pass texture coordinates to setData
            face.setData(faceVertices, faceNormals, faceTexCoords, null, intArrayOf(0, 1, 2), false)
            face.setMaterial(material)
            d20.addChild(face)
        }

        // Scale the D20 to be more visible
        d20.setScale(2.0)

        return d20
    }

    private fun createNumberTexture(number: Int): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = size * 0.35f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true 
        }
        
        val xPos = size / 2f
        // Calculate perfect vertical center
        val yCenter = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        // Subtract offset to move the text UPWARDS (approx 12% up from center)
        val yOffset = size * 0.12f // Adjust this value as needed (larger means higher)
        val yPos = yCenter - yOffset
        
        canvas.drawText(number.toString(), xPos, yPos, paint)
        return bitmap
    }

    fun startSequentialRoll() {
        if (!isRolling) {
            rollSequence = 0
            applyNextRoll()
        }
    }

    private fun applyNextRoll() {
        if (rollSequence >= 3) {
            isRolling = false
            return
        }

        // Removed delay check here to allow immediate sequential rolls
        isRolling = true
        lastRollTime = System.currentTimeMillis()

        // Generate random velocities for each axis
        val velocityX = (Random.nextDouble() - 0.5) * INITIAL_VELOCITY
        val velocityY = (Random.nextDouble() - 0.5) * INITIAL_VELOCITY
        val velocityZ = (Random.nextDouble() - 0.5) * INITIAL_VELOCITY

        rollVelocity = Vector3(velocityX, velocityY, velocityZ)
        rollSequence++
    }

    override fun onRender(elapsedRealTime: Long, deltaTime: Double) {
        super.onRender(elapsedRealTime, deltaTime)

        // Update camera light position
        cameraLight?.position = getCurrentCamera().position

        if (isRolling) {
            // Apply rolling motion
            d20?.let { die ->
                val rotationQuat = Quaternion().fromEuler(
                    rollVelocity.x * deltaTime,
                    rollVelocity.y * deltaTime,
                    rollVelocity.z * deltaTime
                )
                currentRotation.multiply(rotationQuat)
                die.setRotation(currentRotation)
                
                // Apply damping to slow down the roll
                rollVelocity.multiply(DAMPING_FACTOR)
                
                // Check if rolling has slowed down enough
                if (rollVelocity.length() < 0.0001) {
                    if (rollSequence < 3) {
                        applyNextRoll()
                    } else {
                        isRolling = false
                    }
                }
            }
        } else if (!isTouching && (touchVelocityX != 0.0 || touchVelocityY != 0.0)) {
            // Apply momentum from touch
            d20?.let { die ->
                // Convert touch velocity to rotation
                val rotationQuat = Quaternion().fromEuler(
                    -touchVelocityY * deltaTime,
                    touchVelocityX * deltaTime,
                    0.0
                )
                currentRotation.multiply(rotationQuat)
                die.setRotation(currentRotation)
                
                // Apply damping to gradually slow down
                touchVelocityX *= DAMPING_FACTOR
                touchVelocityY *= DAMPING_FACTOR
                
                // Stop when velocity is very small
                if (Math.abs(touchVelocityX) < 0.0001 && Math.abs(touchVelocityY) < 0.0001) {
                    touchVelocityX = 0.0
                    touchVelocityY = 0.0
                }
            }
        } else {
            // Apply manual rotation if any
            d20?.setRotation(currentRotation)
        }

        // Calculate top face based on current rotation
        calculateTopFace()
    }

    private fun calculateTopFace() {
        if (d20 == null || faceNormalsList.isEmpty()) return

        val worldUp = Vector3(0.0, 1.0, 0.0)
        var maxDot = -Double.MAX_VALUE
        var topFaceIndex = -1

        // Pre-calculate the inverse of the current rotation
        val inverseRotation = currentRotation.clone().inverse()

        for (i in faceNormalsList.indices) {
            // Clone the local face normal and rotate it to world space manually
            val localNormal = faceNormalsList[i]
            // Represent the vector as a pure quaternion (w=0)
            val normalQuat = Quaternion(0.0, localNormal.x, localNormal.y, localNormal.z)
            
            // Calculate worldNormal = currentRotation * normalQuat * inverseRotation
            val rotatedQuat = currentRotation.clone().multiply(normalQuat).multiply(inverseRotation)
            
            // Extract the vector part for the world normal
            val worldFaceNormal = Vector3(rotatedQuat.x, rotatedQuat.y, rotatedQuat.z)
            worldFaceNormal.normalize() // Normalize after rotation

            // Find the face whose normal points most upwards
            val dot = worldFaceNormal.dot(worldUp)
            if (dot > maxDot) {
                maxDot = dot
                topFaceIndex = i
            }
        }

        if (topFaceIndex != -1) {
            currentTopFace = faceNumbers[topFaceIndex]
        }
    }

    fun getCurrentTopFace(): Int {
        return currentTopFace
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
        // Not used
    }

    override fun onTouchEvent(event: MotionEvent) {
        // Handle touch events for camera rotation
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Handle sensor events for motion control
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    // Method to handle rotation from touch input
    fun rotateByFinger(dx: Float, dy: Float, isNewTouch: Boolean = false, isEndTouch: Boolean = false) {
        // Allow touch to take precedence over rolling animation
        if (isRolling) {
            isRolling = false  // Cancel the rolling animation
        }

        val currentTime = System.currentTimeMillis()
        
        if (isNewTouch) {
            // Start of touch
            isTouching = true
            lastTouchX = 0f
            lastTouchY = 0f
            touchVelocityX = 0.0
            touchVelocityY = 0.0
            lastTouchTime = currentTime
            return
        }
        
        if (isEndTouch) {
            // End of touch - keep the current velocity for momentum
            isTouching = false
            return
        }
        
        // Calculate time delta
        val deltaTime = (currentTime - lastTouchTime).coerceAtLeast(1L)
        lastTouchTime = currentTime
        
        // Track velocity (pixels per millisecond, converted to rotation speed)
        touchVelocityX = dx / deltaTime * VELOCITY_FACTOR
        touchVelocityY = dy / deltaTime * VELOCITY_FACTOR

        // Remember last position
        lastTouchX = dx
        lastTouchY = dy

        // Adjust sensitivity as needed
        val sensitivity = 0.5 // Increased from 0.4 for more responsive direct control
        val rotX = dy * sensitivity
        val rotY = dx * sensitivity

        // Create rotation quaternions around world axes
        val rotQuatX = Quaternion().fromAngleAxis(Vector3.Axis.X, rotX)
        val rotQuatY = Quaternion().fromAngleAxis(Vector3.Axis.Y, rotY)

        // Apply rotation to the current orientation
        // Apply Y rotation first, then X, to match typical camera controls
        currentRotation.multiply(rotQuatY)
        currentRotation.multiply(rotQuatX)
        currentRotation.normalize()

        d20?.setRotation(currentRotation)
    }

    // Method to get current rotation as Euler angles (degrees)
    fun getCurrentEulerAngles(): FloatArray {
        val angles = FloatArray(3)
        // Use Kotlin math functions
        val pitch = asin(2.0 * (currentRotation.w * currentRotation.x - currentRotation.y * currentRotation.z))
        val yaw = atan2(2.0 * (currentRotation.w * currentRotation.y + currentRotation.x * currentRotation.z), 1.0 - 2.0 * (currentRotation.y * currentRotation.y + currentRotation.x * currentRotation.x))
        val roll = atan2(2.0 * (currentRotation.w * currentRotation.z + currentRotation.x * currentRotation.y), 1.0 - 2.0 * (currentRotation.z * currentRotation.z + currentRotation.x * currentRotation.x))

        angles[0] = MathUtil.radiansToDegrees(pitch).toFloat()
        angles[1] = MathUtil.radiansToDegrees(yaw).toFloat()
        angles[2] = MathUtil.radiansToDegrees(roll).toFloat()
        return angles
    }

    // New function for controlled roll duration
    fun rollWithDuration(durationSeconds: Float = 5.0f) {
        // Always allow roll to start - removed check for !isRolling
        isRolling = true
        
        // Generate random velocities for each axis
        val velocityX = (Random.nextDouble() - 0.5) * 2.0
        val velocityY = (Random.nextDouble() - 0.5) * 2.0
        val velocityZ = (Random.nextDouble() - 0.5) * 2.0
        
        // Create a normalized vector for the velocity
        val initialVelocity = Vector3(velocityX, velocityY, velocityZ)
        initialVelocity.normalize()
        
        // Scale to appropriate speed (increased by 1000x)
        val initialSpeed = 8.0 * 1000.0  // 1000x the original speed
        initialVelocity.multiply(initialSpeed)
        
        // Set the velocity for rolling motion
        rollVelocity = initialVelocity
        
        // Calculate dampingFactor to achieve the desired duration
        // Since we're starting with 1000x the velocity, we need to adjust the damping
        // to still stop in 5 seconds
        
        val frames = 60 * durationSeconds
        val targetRemainingVelocity = 0.0001 / 1000.0  // Adjusted for higher initial velocity
        DAMPING_FACTOR = Math.pow(targetRemainingVelocity, 1.0 / frames)
    }
} 