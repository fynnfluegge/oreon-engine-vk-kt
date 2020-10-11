package org.oreon.vk.components.atmosphere

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.math.Vec3f
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.scenegraph.Renderable
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.getCamera
import org.oreon.core.vk.context.VkContext.resources
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import org.oreon.core.vk.wrapper.command.SecondaryDrawCmdBuffer
import org.oreon.core.vk.wrapper.image.VkImageBundle
import org.oreon.core.vk.wrapper.image.VkImageHelper
import org.oreon.core.vk.wrapper.pipeline.GraphicsPipeline
import java.util.*

class Sun : Renderable() {
    private val sunImageBundle: VkImageBundle
    private val sunImageBundle_lightScattering: VkImageBundle
    override fun shutdown() {
        super.shutdown()
        sunImageBundle.destroy()
        sunImageBundle_lightScattering.destroy()
    }

    init {
        val device = deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val memoryProperties = deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        worldTransform!!.translation = config.sunPosition!!.normalize().mul(-2600f)
        val origin = Vec3f(0f, 0f, 0f)
        val array = arrayOfNulls<Vec3f>(1)
        array[0] = origin
        val sunImage = VkImageHelper.loadImageFromFile(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                "textures/sun/sun.png",
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                VK10.VK_QUEUE_GRAPHICS_BIT)
        val sunImageView = VkImageView(device.handle,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, sunImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val sunImageSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        sunImageBundle = VkImageBundle(sunImage, sunImageView, sunImageSampler)
        val sunImage_lightScattering = VkImageHelper.loadImageFromFile(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                "textures/sun/sun_small.png",
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                VK10.VK_QUEUE_GRAPHICS_BIT)
        val sunImageView_lightScattering = VkImageView(device.handle,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, sunImage_lightScattering.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val sunImageSampler_lightScattering = VkSampler(device.handle, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        sunImageBundle_lightScattering = VkImageBundle(sunImage_lightScattering,
                sunImageView_lightScattering, sunImageSampler_lightScattering)
        val vertexInput = VkVertexInput(VertexLayout.POS)
        val vertexBuffer = BufferUtil.createByteBuffer(array)
        val vertexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                vertexBuffer, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        val descriptorSetLayout = DescriptorSetLayout(device.handle, 2)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.create()
        val descriptorSet = DescriptorSet(device.handle,
                device.getDescriptorPool(Thread.currentThread().id)!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(
                sunImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                sunImageSampler.handle, 0,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                sunImageView_lightScattering.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                sunImageSampler_lightScattering.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSets.add(getCamera().descriptorSet)
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(getCamera().descriptorSetLayout)
        descriptorSetLayouts.add(descriptorSetLayout)
        val pushConstantRange = java.lang.Float.BYTES * 16
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.put(BufferUtil.createByteBuffer(worldTransform!!.worldMatrix))
        pushConstants.flip()
        val shaderPipeline = ShaderPipeline(device.handle)
        shaderPipeline.createVertexShader("shaders/sun/sun.vert.spv")
        shaderPipeline.createFragmentShader("shaders/sun/sun.frag.spv")
        shaderPipeline.createShaderPipeline()
        val graphicsPipeline: VkPipeline = GraphicsPipeline(device.handle,
                shaderPipeline, vertexInput, VK10.VK_PRIMITIVE_TOPOLOGY_POINT_LIST,
                VkUtil.createLongBuffer(descriptorSetLayouts),
                config.frameWidth,
                config.frameHeight,
                resources.transparencyFbo!!.renderPass!!.handle,
                resources.transparencyFbo!!.colorAttachmentCount,
                1,
                pushConstantRange, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        val mainCommandBuffer: CommandBuffer = SecondaryDrawCmdBuffer(
                device.handle,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                graphicsPipeline.handle, graphicsPipeline.layoutHandle,
                resources.transparencyFbo!!.frameBuffer!!.handle,
                resources.transparencyFbo!!.renderPass!!.handle,
                0,
                VkUtil.createLongArray(descriptorSets),
                vertexBufferObject.handle, 1,
                pushConstants, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        val meshData = VkMeshData.builder().vertexBufferObject(vertexBufferObject)
                .vertexBuffer(vertexBuffer).build()

        val mainRenderInfo = VkRenderInfo(commandBuffer = mainCommandBuffer, pipeline = graphicsPipeline,
                descriptorSets = descriptorSets, descriptorSetLayouts = descriptorSetLayouts)
        addComponent(NodeComponentType.MESH_DATA, meshData)
        addComponent(NodeComponentType.MAIN_RENDERINFO, mainRenderInfo);
        shaderPipeline.destroy()
    }
}