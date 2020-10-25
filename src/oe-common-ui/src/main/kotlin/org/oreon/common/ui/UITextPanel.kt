package org.oreon.common.ui

import mu.KotlinLogging
import org.oreon.common.ui.UIPanelLoader.load
import org.oreon.core.math.Vec2f
import org.oreon.core.model.Mesh
import org.oreon.core.model.Vertex
import org.oreon.core.util.Util
import org.oreon.core.util.Util.texCoordsFromFontMap
import org.oreon.core.util.Util.toIntArray
import java.util.*

abstract class UITextPanel(text: String, xPos: Int, yPos: Int, xScaling: Int, yScaling: Int) : UIElement(xPos, yPos, xScaling, yScaling) {

    private val logger = KotlinLogging.logger {}

    protected var panel: Mesh
    protected var outputText: String?
    private val numFonts: Int

    fun generatePanelVertices(): Mesh {
        val vertexList: MutableList<Vertex> = ArrayList()
        val indexList: MutableList<Int> = ArrayList()
        for (i in 0 until numFonts) {
            val mesh = load("gui/basicPanel.gui")
            for (v in mesh!!.vertices) {
                v.position.x = v.position.x + i * 0.65f
                vertexList.add(v)
            }
            for (index in mesh.indices) {
                indexList.add(index + 4 * i)
            }
        }
        val indices: IntArray = toIntArray(indexList)
        return Mesh(Util.toVertexArray(vertexList), indices)
    }

    fun generateFontMapUvCoords(text: String?) {
        val uvList: MutableList<Vec2f?> = ArrayList()
        for (i in 0 until numFonts) {
            var fontMapUv = arrayOfNulls<Vec2f>(4)
            fontMapUv = texCoordsFromFontMap(text!![i])
            uvList.add(fontMapUv[0])
            uvList.add(fontMapUv[1])
            uvList.add(fontMapUv[2])
            uvList.add(fontMapUv[3])
        }
        if (uvList.size != panel.vertices.size) {
            logger.error("uv count not equal vertex count")
        }
        var i = 0
        for (v in panel.vertices) {
            v.uvCoord = uvList[i]!!
            i++
        }
    }

    override fun update(newText: String?) {
        outputText = newText
        var textToDisplay: String? = String()
        if (newText!!.length > numFonts) {
            logger.error("Text to update too long")
        }
        if (newText.length < numFonts) {
            val offset = numFonts - newText.length
            for (i in 0 until offset) {
                textToDisplay += " "
            }
        }
        textToDisplay += newText
        generateFontMapUvCoords(textToDisplay)
    }

    init {
        outputText = text
        numFonts = text.length
        panel = generatePanelVertices()
        generateFontMapUvCoords(text)
    }
}