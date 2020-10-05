package org.oreon.core.util

import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBImage
import org.oreon.core.context.BaseContext.Companion.config
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.jvm.Throws

object ResourceLoader {
    fun loadShader(fileName: String): String {
        val `is` = ResourceLoader::class.java.classLoader.getResourceAsStream(fileName)
        val shaderSource = StringBuilder()
        var shaderReader: BufferedReader? = null
        try {
            shaderReader = BufferedReader(InputStreamReader(`is`))
            var line: String?
            while (shaderReader.readLine().also { line = it } != null) {
                shaderSource.append(line).append("\n")
            }
            shaderReader.close()
        } catch (e: Exception) {
            System.err.println("Unable to load file [$fileName]!")
            e.printStackTrace()
            System.exit(1)
        }
        return shaderSource.toString()
    }

    fun loadShader(fileName: String, lib: String): String {
        val shadersource = loadShader(fileName)
        val `is` = ResourceLoader::class.java.classLoader.getResourceAsStream("shader/$lib")
        val shaderlibSource = StringBuilder()
        var shaderReader: BufferedReader? = null
        try {
            shaderReader = BufferedReader(InputStreamReader(`is`))
            var line: String?
            while (shaderReader.readLine().also { line = it } != null) {
                shaderlibSource.append(line).append("\n")
            }
            shaderReader.close()
        } catch (e: Exception) {
            System.err.println("Unable to load file [$fileName]!")
            e.printStackTrace()
            System.exit(1)
        }

        // replace const paramter in glsl library
        val vlib = shaderlibSource.toString().replaceFirst("#var_shadow_map_resolution".toRegex(), Integer.toString(config.shadowMapResolution))
        return shadersource.replaceFirst("#lib.glsl".toRegex(), vlib)
    }

    fun loadImageToByteBuffer(file: String?): ByteBuffer {
        val imageBuffer: ByteBuffer
        imageBuffer = try {
            ioResourceToByteBuffer(file, 128 * 128)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val w = BufferUtils.createIntBuffer(1)
        val h = BufferUtils.createIntBuffer(1)
        val c = BufferUtils.createIntBuffer(1)

        // Use info to read image metadata without decoding the entire image.
        if (!STBImage.stbi_info_from_memory(imageBuffer, w, h, c)) {
            throw RuntimeException("Failed to read image information: " + STBImage.stbi_failure_reason())
        }

//        System.out.println("Image width: " + w.get(0));
//        System.out.println("Image height: " + h.get(0));
//        System.out.println("Image components: " + c.get(0));
//        System.out.println("Image HDR: " + stbi_is_hdr_from_memory(imageBuffer));

        // Decode the image
        return STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 0)
                ?: throw RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason())
    }

    @Throws(IOException::class)
    fun ioResourceToByteBuffer(resource: String?, bufferSize: Int): ByteBuffer {
        var buffer: ByteBuffer
        val path = Paths.get(resource)
        if (Files.isReadable(path)) {
            Files.newByteChannel(path).use { fc ->
                buffer = BufferUtils.createByteBuffer(fc.size().toInt() + 1)
                while (fc.read(buffer) != -1) {
                }
            }
        } else {
            ResourceLoader::class.java.classLoader.getResourceAsStream(resource).use { source ->
                Channels.newChannel(source).use { rbc ->
                    buffer = BufferUtils.createByteBuffer(bufferSize)
                    while (true) {
                        val bytes = rbc.read(buffer)
                        if (bytes == -1) {
                            break
                        }
                        if (buffer.remaining() == 0) {
                            buffer = BufferUtil.resizeBuffer(buffer, buffer.capacity() * 2)
                        }
                    }
                }
            }
        }
        buffer.flip()
        return buffer
    }
}