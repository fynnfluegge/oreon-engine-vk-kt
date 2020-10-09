package org.oreon.core.vk.image

import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBImage
import org.oreon.core.image.ImageMetaData
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths

object VkImageLoader {
    fun getImageMetaData(file: String?): ImageMetaData {
        val imageBuffer: ByteBuffer
        imageBuffer = try {
            ioResourceToByteBuffer(file, 128 * 128)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val x = BufferUtils.createIntBuffer(1)
        val y = BufferUtils.createIntBuffer(1)
        val channels = BufferUtils.createIntBuffer(1)

        // Use info to read image metadata without decoding the entire image.
        if (!STBImage.stbi_info_from_memory(imageBuffer, x, y, channels)) {
            throw RuntimeException("Failed to read image information: " + STBImage.stbi_failure_reason())
        }
        return ImageMetaData(x[0], y[0], channels[0])
    }

    fun decodeImage(file: String?): ByteBuffer? {
        var absolutePath = VkImageLoader::class.java.classLoader.getResource(file).path.substring(1)
        if (!System.getProperty("os.name").contains("Windows")) { // TODO Language/region agnostic value for 'Windows' ?
            // stbi_load requires a file system path, NOT a classpath resource path
            absolutePath = File.separator + absolutePath
        }
        val x = BufferUtils.createIntBuffer(1)
        val y = BufferUtils.createIntBuffer(1)
        val channels = BufferUtils.createIntBuffer(1)
        val image = STBImage.stbi_load(absolutePath, x, y, channels, STBImage.STBI_rgb_alpha)
        if (image == null) {
            System.err.println("Could not decode image file [" + absolutePath + "]: [" + STBImage.stbi_failure_reason() + "]")
        }
        return image
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
            VkImageLoader::class.java.classLoader.getResourceAsStream(resource).use { source ->
                Channels.newChannel(source).use { rbc ->
                    buffer = BufferUtils.createByteBuffer(bufferSize)
                    while (true) {
                        val bytes = rbc.read(buffer)
                        if (bytes == -1) {
                            break
                        }
                        if (buffer.remaining() == 0) {
                            buffer = resizeBuffer(buffer, buffer.capacity() * 2)
                        }
                    }
                }
            }
        }
        buffer.flip()
        return buffer
    }

    private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
        val newBuffer = BufferUtils.createByteBuffer(newCapacity)
        buffer.flip()
        newBuffer.put(buffer)
        return newBuffer
    }
}