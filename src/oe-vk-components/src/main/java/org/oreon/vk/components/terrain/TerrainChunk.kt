package org.oreon.vk.components.terrain

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.common.quadtree.QuadtreeCache
import org.oreon.common.quadtree.QuadtreeNode
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.math.Transform
import org.oreon.core.math.Vec2f
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.util.BufferUtil.createByteBuffer
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.resources
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.util.VkUtil.createLongBuffer
import org.oreon.core.vk.wrapper.command.SecondaryDrawCmdBuffer
import org.oreon.core.vk.wrapper.pipeline.GraphicsTessellationPipeline

class TerrainChunk(components: Map<NodeComponentType, NodeComponent>, quadtreeCache: QuadtreeCache?,
                   worldTransform: Transform, location: Vec2f?, levelOfDetail: Int, index: Vec2f?) : QuadtreeNode(components, quadtreeCache, worldTransform, location, levelOfDetail, index) {
    override fun createChildChunk(components: Map<NodeComponentType, NodeComponent>, quadtreeCache: QuadtreeCache,
                                  worldTransform: Transform, location: Vec2f, levelOfDetail: Int, index: Vec2f): QuadtreeNode {
        return TerrainChunk(components, quadtreeCache, worldTransform, location, levelOfDetail, index)
    }

    init {
        try {
            addComponent(NodeComponentType.MAIN_RENDERINFO, components[NodeComponentType.MAIN_RENDERINFO]!!.clone())
            addComponent(NodeComponentType.MESH_DATA, components[NodeComponentType.MESH_DATA]!!.clone())
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }
        val device = deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val renderInfo: VkRenderInfo = getComponent(NodeComponentType.MAIN_RENDERINFO)!!
        val meshData: VkMeshData = getComponent(NodeComponentType.MESH_DATA)!!
        val pushConstantsRange = java.lang.Float.BYTES * 42 + Integer.BYTES * 11
        val pushConstants = MemoryUtil.memAlloc(pushConstantsRange)
        pushConstants.put(createByteBuffer(localTransform!!.worldMatrix))
        pushConstants.put(createByteBuffer(worldTransform.worldMatrixRTS))
        pushConstants.putFloat(quadtreeConfig.verticalScaling)
        pushConstants.putFloat(quadtreeConfig.horizontalScaling)
        pushConstants.putInt(chunkConfig.lod)
        pushConstants.putFloat(chunkConfig.gap)
        pushConstants.put(createByteBuffer(location!!))
        pushConstants.put(createByteBuffer(index!!))
        for (morphArea in quadtreeConfig.lod_morphing_area) {
            pushConstants.putInt(morphArea)
        }
        pushConstants.putInt(quadtreeConfig.tessellationFactor)
        pushConstants.putFloat(quadtreeConfig.tessellationSlope)
        pushConstants.putFloat(quadtreeConfig.tessellationShift)
        pushConstants.putFloat(quadtreeConfig.uvScaling)
        pushConstants.putInt(quadtreeConfig.highDetailRange)
        pushConstants.flip()
        val graphicsPipeline: VkPipeline = GraphicsTessellationPipeline(device.handle,
                renderInfo.shaderPipeline, renderInfo.vertexInput,
                createLongBuffer(renderInfo.descriptorSetLayouts!!),
                config.frameWidth,
                config.frameHeight,
                resources.offScreenFbo!!.renderPass!!.handle,
                resources.offScreenFbo!!.colorAttachmentCount,
                config.multisampling_sampleCount,
                pushConstantsRange, VK10.VK_SHADER_STAGE_ALL_GRAPHICS,
                16)
        val commandBuffer: CommandBuffer = SecondaryDrawCmdBuffer(
                device.handle,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                graphicsPipeline.handle, graphicsPipeline.layoutHandle,
                resources.offScreenFbo!!.frameBuffer!!.handle,
                resources.offScreenFbo!!.renderPass!!.handle,
                0,
                createLongArray(renderInfo.descriptorSets!!),
                meshData.vertexBufferObject!!.handle,
                meshData.vertexCount!!,
                pushConstants, VK10.VK_SHADER_STAGE_ALL_GRAPHICS)
        renderInfo.commandBuffer = commandBuffer
        renderInfo.pipeline = graphicsPipeline
    }
}