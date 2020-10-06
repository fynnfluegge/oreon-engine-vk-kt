package org.oreon.core.instanced

import org.oreon.core.context.BaseContext.Companion.camera
import org.oreon.core.math.Matrix4f
import org.oreon.core.math.Vec3f
import org.oreon.core.scenegraph.RenderList
import org.oreon.core.scenegraph.Renderable
import org.oreon.core.util.IntegerReference
import java.util.*
import java.util.function.Consumer

abstract class InstancedObject : Renderable() {
    private val instanceCount = 0
    private val positions: Array<Vec3f> = TODO()
    private val worldMatrices: List<Matrix4f> = ArrayList()
    private val modelMatrices: List<Matrix4f> = ArrayList()
    private val highPolyIndices: ArrayList<Int> = ArrayList()
    private val lowPolyIndices: List<Int> = ArrayList()
    private val lowPolyObjects: List<Renderable> = ArrayList()
    private val highPolyObjects: List<Renderable> = ArrayList()
    private val highPolyInstanceCount: IntegerReference? = null
    private val lowPolyInstanceCount: IntegerReference? = null
    private val highPolyRange = 0

    override fun update() {

        highPolyIndices.clear()
        var index = 0
        for (transform in worldMatrices) {
            if (transform.translation.sub(camera.position).length() < highPolyRange) {
                highPolyIndices.add(index)
            }
            index++
        }
        highPolyInstanceCount?.value = highPolyIndices.size
    }

    override fun record(renderList: RenderList?) {
        if (render) {
            if (!renderList!!.contains(id)) {
                renderList!!.add(this)
                renderList!!.changed = true
            }
        } else {
            if (renderList!!.contains(id)) {
                renderList!!.remove(this)
                renderList!!.changed = true;
            }
        }
    }

    fun renderLowPoly() {
        lowPolyObjects.forEach(Consumer { `object`: Renderable -> `object`.render() })
    }

    fun renderHighPoly() {
        highPolyObjects.forEach(Consumer { `object`: Renderable -> `object`.render() })
    }

    fun renderLowPolyShadows() {
        lowPolyObjects.forEach(Consumer { `object`: Renderable -> `object`.renderShadows() })
    }

    fun renderHighPolyShadows() {
        highPolyObjects.forEach(Consumer { `object`: Renderable -> `object`.renderShadows() })
    }
}