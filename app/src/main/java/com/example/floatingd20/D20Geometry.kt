package com.example.floatingd20

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object D20Geometry {
    // Constants for icosahedron construction
    private const val X = 0.525731112119133606f
    private const val Z = 0.850650808352039932f
    private const val N = 0.0f

    fun createVertices(): FloatBuffer {
        // These vertices define a regular icosahedron
        val vertices = floatArrayOf(
            -X, N, Z,  X, N, Z,  -X, N, -Z,  X, N, -Z,
            N, Z, X,  N, Z, -X,  N, -Z, X,  N, -Z, -X,
            Z, X, N,  -Z, X, N,  Z, -X, N,  -Z, -X, N
        )

        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(vertices)
        buffer.rewind()
        return buffer
    }

    fun createIndices(): IntBuffer {
        // These indices define the 20 triangular faces of the icosahedron
        val indices = intArrayOf(
            0,4,1,  0,9,4,  9,5,4,  4,5,8,  4,8,1,
            8,10,1, 8,3,10, 5,3,8,  5,2,3,  2,7,3,
            7,10,3, 7,6,10, 7,11,6, 11,0,6, 0,1,6,
            6,1,10, 9,0,11, 9,11,2, 9,2,5,  7,2,11
        )

        val buffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        buffer.put(indices)
        buffer.rewind()
        return buffer
    }

    fun createNormals(): FloatBuffer {
        // Calculate normals for proper lighting
        val vertices = createVertices()
        val indices = createIndices()
        val vertexArray = FloatArray(vertices.limit())
        vertices.get(vertexArray)
        val indexArray = IntArray(indices.limit())
        indices.get(indexArray)

        val normals = FloatArray(vertexArray.size)
        for (i in 0 until indexArray.size step 3) {
            val v1x = vertexArray[indexArray[i] * 3]
            val v1y = vertexArray[indexArray[i] * 3 + 1]
            val v1z = vertexArray[indexArray[i] * 3 + 2]
            
            val v2x = vertexArray[indexArray[i + 1] * 3]
            val v2y = vertexArray[indexArray[i + 1] * 3 + 1]
            val v2z = vertexArray[indexArray[i + 1] * 3 + 2]
            
            val v3x = vertexArray[indexArray[i + 2] * 3]
            val v3y = vertexArray[indexArray[i + 2] * 3 + 1]
            val v3z = vertexArray[indexArray[i + 2] * 3 + 2]

            // Calculate face normal using cross product
            val ux = v2x - v1x
            val uy = v2y - v1y
            val uz = v2z - v1z
            
            val vx = v3x - v1x
            val vy = v3y - v1y
            val vz = v3z - v1z
            
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx
            
            // Normalize
            val length = sqrt(nx * nx + ny * ny + nz * nz)
            val normalX = nx / length
            val normalY = ny / length
            val normalZ = nz / length

            // Add normal to each vertex
            for (j in 0..2) {
                val idx = indexArray[i + j] * 3
                normals[idx] = normalX
                normals[idx + 1] = normalY
                normals[idx + 2] = normalZ
            }
        }

        val buffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(normals)
        buffer.rewind()
        return buffer
    }

    fun createUVs(): FloatBuffer {
        // Create UV coordinates for texturing
        val uvs = FloatArray(12 * 2) // 12 vertices, 2 coordinates each
        for (i in 0 until 12) {
            val angle = 2.0f * PI.toFloat() * i / 12.0f
            uvs[i * 2] = (cos(angle) * 0.5f + 0.5f)
            uvs[i * 2 + 1] = (sin(angle) * 0.5f + 0.5f)
        }

        val buffer = ByteBuffer.allocateDirect(uvs.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(uvs)
        buffer.rewind()
        return buffer
    }
} 