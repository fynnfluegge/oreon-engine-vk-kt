package org.oreon.common.quadtree

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.Setter
import org.oreon.core.math.Vec2f

@Getter
@Setter
@AllArgsConstructor
class ChunkConfig {
    private val lod = 0
    private val location: Vec2f? = null
    private val index: Vec2f? = null
    private val gap = 0f
}