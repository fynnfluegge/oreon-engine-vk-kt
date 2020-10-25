package org.oreon.common.quadtree

import org.oreon.core.scenegraph.Node
import java.util.*

class QuadtreeCache {
    private val chunks: HashMap<String, QuadtreeNode> = HashMap()
    operator fun contains(key: String): Boolean {
        return chunks.containsKey(key)
    }

    fun addChunk(chunk: Node) {
        chunks[(chunk as QuadtreeNode).quadtreeCacheKey] = chunk
    }

    fun addChunk(chunk: QuadtreeNode) {
        chunks[chunk.quadtreeCacheKey] = chunk
    }

    fun getChunk(key: String): QuadtreeNode? {
        return chunks[key]
    }

    fun removeChunk(key: String) {
        chunks.remove(key)
    }

    fun getAndRemoveChunk(key: String): QuadtreeNode? {
        val chunk = chunks[key]
        chunks.remove(key)
        return chunk
    }

}