package org.oreon.vk.components.planet

import org.lwjgl.vulkan.VK10
import org.oreon.common.quadtree.Quadtree
import org.oreon.common.quadtree.QuadtreeConfig
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.scenegraph.Node
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.util.BufferUtil
import org.oreon.core.util.MeshGenerator
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.getCamera
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import java.util.*

class Planet : Node() {

    val quadtree: Quadtree

    override fun render() {
        return
    }

    init {
        val device = deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val memoryProperties = deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        val mesh = MeshGenerator.TerrainChunkMesh()
        val vertexBuffer = BufferUtil.createByteBuffer(mesh)
        val vertexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                vertexBuffer, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        val meshData = VkMeshData.builder().vertexBufferObject(vertexBufferObject)
                .vertexBuffer(vertexBuffer).vertexCount(mesh.size).build()
        val components = HashMap<NodeComponentType, NodeComponent>()
        val config = QuadtreeConfig()
        val vertexInput = VkVertexInput(VertexLayout.POS2D)
        val shaderPipeline = ShaderPipeline(device.handle)
        shaderPipeline.createVertexShader("shaders/planet/planet.vert.spv")
        shaderPipeline.createTessellationControlShader("shaders/planet/planet.tesc.spv")
        shaderPipeline.createTessellationEvaluationShader("shaders/planet/planet.tese.spv")
        shaderPipeline.createGeometryShader("shaders/planet/planetWireframe.geom.spv")
        shaderPipeline.createFragmentShader("shaders/planet/planet.frag.spv")
        shaderPipeline.createShaderPipeline()
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(getCamera().descriptorSet)
        descriptorSetLayouts.add(getCamera().descriptorSetLayout)

	    val renderInfo = VkRenderInfo(vertexInput = vertexInput, descriptorSets = descriptorSets,
                descriptorSetLayouts = descriptorSetLayouts, shaderPipeline = shaderPipeline)
        components[NodeComponentType.CONFIGURATION] = config
        components[NodeComponentType.MAIN_RENDERINFO] = renderInfo;
        components[NodeComponentType.MESH_DATA] = meshData
        val planetQuadtree = PlanetQuadtree(components, config,
                config.rootChunkCount, config.horizontalScaling)
        quadtree = planetQuadtree
        addChild(planetQuadtree)
        planetQuadtree.start()
    }
}