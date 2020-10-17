package org.oreon.vk.components.water

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkQueue
import org.oreon.common.water.WaterConfig
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.math.Vec4f
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.scenegraph.RenderList
import org.oreon.core.scenegraph.Renderable
import org.oreon.core.scenegraph.Scenegraph
import org.oreon.core.target.FrameBufferObject
import org.oreon.core.util.BufferUtil
import org.oreon.core.util.Constants
import org.oreon.core.util.MeshGenerator
import org.oreon.core.util.Util.getLog2N
import org.oreon.core.util.Util.getMipLevelCount
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.getCamera
import org.oreon.core.vk.context.VkContext.resources
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.framebuffer.FrameBufferColorAttachment
import org.oreon.core.vk.framebuffer.FrameBufferDepthAttachment
import org.oreon.core.vk.framebuffer.VkFrameBuffer
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.RenderPass
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import org.oreon.core.vk.wrapper.buffer.VkUniformBuffer
import org.oreon.core.vk.wrapper.command.PrimaryCmdBuffer
import org.oreon.core.vk.wrapper.command.SecondaryDrawCmdBuffer
import org.oreon.core.vk.wrapper.image.VkImageBundle
import org.oreon.core.vk.wrapper.image.VkImageHelper
import org.oreon.core.vk.wrapper.pipeline.GraphicsTessellationPipeline
import org.oreon.vk.components.fft.FFT
import org.oreon.vk.components.util.NormalRenderer
import java.util.*

class Water : Renderable() {

    private val waterConfig: WaterConfig
    private var systemTime = System.currentTimeMillis()
    private val fft: FFT
    private val normalRenderer: NormalRenderer
    private val clipplane: Vec4f
    private val clip_offset: Float
    private var motion = 0f
    private var distortion = 0f
    private val image_dudv: VkImage
    private val imageView_dudv: VkImageView
    private val uniformBuffer: VkUniformBuffer
    private val dxSampler: VkSampler
    private val dySampler: VkSampler
    private val dzSampler: VkSampler
    private val dudvSampler: VkSampler
    private val normalSampler: VkSampler
    private val reflectionSampler: VkSampler
    private val refractionSampler: VkSampler

    // Reflection/Refraction Resources
    private val reflectionFbo: VkFrameBufferObject
    private val refractionFbo: VkFrameBufferObject
    private val offScreenReflectionRenderList: RenderList
    private val reflectionSecondaryCmdBuffers: LinkedHashMap<String, CommandBuffer?>
    private val offscreenReflectionCmdBuffer: PrimaryCmdBuffer
    private val offScreenReflectionSubmitInfo: SubmitInfo
    private val reflectionFence: Fence
    private val offScreenRefractionRenderList: RenderList
    private val refractionSecondaryCmdBuffers: LinkedHashMap<String, CommandBuffer?>
    private val offscreenRefractionCmdBuffer: PrimaryCmdBuffer
    private val offScreenRefractionSubmitInfo: SubmitInfo
    private val refractionFence: Fence

    // queues for render reflection/refraction
    private val graphicsQueue: VkQueue
    override fun render() {
        fft.render()
        normalRenderer.render(VK10.VK_QUEUE_FAMILY_IGNORED)

        // render reflection
        config.clipplane = clipplane

        // mirror scene to clipplane
        val sceneGraph = getParentObject<Scenegraph>()
        sceneGraph!!.worldTransform!!.setScaling(1f, -1f, 1f)
        sceneGraph.update()
        sceneGraph.root.record(offScreenReflectionRenderList)
        for (key in offScreenReflectionRenderList.keySet) {
            if (!reflectionSecondaryCmdBuffers.containsKey(key)) {
                if (offScreenReflectionRenderList[key]!!.getComponents()
                                .containsKey(NodeComponentType.REFLECTION_RENDERINFO)) {
                    val renderInfo: VkRenderInfo? = offScreenReflectionRenderList[key]!!.getComponent(NodeComponentType.REFLECTION_RENDERINFO)
                    reflectionSecondaryCmdBuffers[key] = renderInfo?.commandBuffer
                }
            }
        }

        // render reflection scene
        if (!reflectionSecondaryCmdBuffers.isEmpty()) {
            offscreenReflectionCmdBuffer.reset()
            offscreenReflectionCmdBuffer.record(
                    reflectionFbo.renderPass!!.handle,
                    reflectionFbo.frameBuffer!!.handle,
                    reflectionFbo.width,
                    reflectionFbo.height,
                    reflectionFbo.colorAttachmentCount,
                    reflectionFbo.depthAttachmentCount,
                    VkUtil.createPointerBuffer(reflectionSecondaryCmdBuffers.values))
            offScreenReflectionSubmitInfo.submit(
                    graphicsQueue)
        }
        reflectionFence.waitForFence()

        // antimirror scene to clipplane
        sceneGraph.worldTransform!!.setScaling(1f, 1f, 1f)
        sceneGraph.update()
        sceneGraph.root.record(offScreenRefractionRenderList)
        for (key in offScreenRefractionRenderList.keySet) {
            if (!refractionSecondaryCmdBuffers.containsKey(key)) {
                if (offScreenRefractionRenderList[key]!!.getComponents()
                                .containsKey(NodeComponentType.REFRACTION_RENDERINFO)) {
                    val renderInfo: VkRenderInfo? = offScreenRefractionRenderList[key]!!.getComponent(NodeComponentType.REFRACTION_RENDERINFO)
                    refractionSecondaryCmdBuffers[key] = renderInfo?.commandBuffer
                }
            }
        }

        // render refraction scene
        if (!refractionSecondaryCmdBuffers.isEmpty()) {
            offscreenRefractionCmdBuffer.reset()
            offscreenRefractionCmdBuffer.record(
                    refractionFbo.renderPass!!.handle,
                    refractionFbo.frameBuffer!!.handle,
                    refractionFbo.width,
                    refractionFbo.height,
                    refractionFbo.colorAttachmentCount,
                    refractionFbo.depthAttachmentCount,
                    waterConfig.baseColor.mul(2.5f),
                    VkUtil.createPointerBuffer(refractionSecondaryCmdBuffers.values))
            offScreenRefractionSubmitInfo.submit(
                    graphicsQueue)
        }
        refractionFence.waitForFence()
        motion += (System.currentTimeMillis() - systemTime) * waterConfig.waveMotion
        distortion += (System.currentTimeMillis() - systemTime) * waterConfig.distortion
        val v = floatArrayOf(motion, distortion)
        uniformBuffer.mapMemory(BufferUtil.createByteBuffer(*v))
        systemTime = System.currentTimeMillis()
    }

    override fun shutdown() {
        super.shutdown()
        fft.destroy()
        normalRenderer.destroy()
        image_dudv.destroy()
        imageView_dudv.destroy()
        uniformBuffer.destroy()
        dxSampler.destroy()
        dySampler.destroy()
        dzSampler.destroy()
        dudvSampler.destroy()
        normalSampler.destroy()
        reflectionSampler.destroy()
        refractionSampler.destroy()
        offscreenReflectionCmdBuffer.destroy()
        offscreenRefractionCmdBuffer.destroy()
        refractionFence.destroy()
        reflectionFence.destroy()
        reflectionFbo.destroy()
        refractionFbo.destroy()
    }

    inner class ReflectionRefractionFbo(device: VkDevice?,
                                        memoryProperties: VkPhysicalDeviceMemoryProperties?) : VkFrameBufferObject() {
        init {
            width = config.frameWidth / 2
            height = config.frameHeight / 2
            val albedoBuffer: VkImageBundle = FrameBufferColorAttachment(device!!, memoryProperties!!, width, height,
                    VK10.VK_FORMAT_R16G16B16A16_SFLOAT, 1)
            val depthBuffer: VkImageBundle = FrameBufferDepthAttachment(device, memoryProperties, width, height,
                    VK10.VK_FORMAT_D16_UNORM, 1)
            attachments[Attachment.COLOR] = albedoBuffer
            attachments[Attachment.DEPTH] = depthBuffer
            renderPass = RenderPass(device)
            renderPass!!.addColorAttachment(0, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    VK10.VK_FORMAT_R16G16B16A16_SFLOAT, 1, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                    VK10.VK_IMAGE_LAYOUT_GENERAL)
            renderPass!!.addDepthAttachment(1, VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                    VK10.VK_FORMAT_D16_UNORM, 1, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                    VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
            renderPass!!.addSubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK10.VK_ACCESS_MEMORY_READ_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                    VK10.VK_DEPENDENCY_BY_REGION_BIT)
            renderPass!!.addSubpassDependency(0, VK10.VK_SUBPASS_EXTERNAL,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                    VK10.VK_ACCESS_MEMORY_READ_BIT,
                    VK10.VK_DEPENDENCY_BY_REGION_BIT)
            renderPass!!.createSubpass()
            renderPass!!.createRenderPass()
            depthAttachmentCount = 1
            colorAttachmentCount = renderPass!!.attachmentCount - depthAttachmentCount
            val pImageViews = MemoryUtil.memAllocLong(renderPass!!.attachmentCount)
            pImageViews.put(0, attachments[Attachment.COLOR]!!.imageView.handle)
            pImageViews.put(1, attachments[Attachment.DEPTH]!!.imageView.handle)
            frameBuffer = VkFrameBuffer(device, width, height, 1,
                    pImageViews, renderPass!!.handle)
        }
    }

    init {
        val deviceBundle = deviceManager.getDeviceBundle(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val device = deviceBundle!!.logicalDevice
        val descriptorPool = device.getDescriptorPool(Thread.currentThread().id)
        val memoryProperties = deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        graphicsQueue = device.graphicsQueue
        offScreenReflectionRenderList = RenderList()
        offScreenRefractionRenderList = RenderList()
        reflectionSecondaryCmdBuffers = LinkedHashMap()
        refractionSecondaryCmdBuffers = LinkedHashMap()
        reflectionFbo = ReflectionRefractionFbo(device.handle, memoryProperties)
        refractionFbo = ReflectionRefractionFbo(device.handle, memoryProperties)
        resources.reflectionFbo = reflectionFbo
        resources.refractionFbo = refractionFbo
        worldTransform!!.setScaling(Constants.ZFAR, 1f, Constants.ZFAR)
        worldTransform!!.setTranslation(-Constants.ZFAR / 2, 0f, -Constants.ZFAR / 2)
        clip_offset = 4f
        clipplane = Vec4f(0f, -1f, 0f, worldTransform!!.translation!!.y + clip_offset)
        waterConfig = WaterConfig()
        waterConfig.loadFile("water-config.properties")
        image_dudv = VkImageHelper.loadImageFromFileMipmap(
                device.handle, memoryProperties,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                device.graphicsQueue,
                "textures/water/dudv/dudv1.jpg",
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                VK10.VK_QUEUE_FAMILY_IGNORED)
        imageView_dudv = VkImageView(device.handle,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, image_dudv.handle,
                VK10.VK_IMAGE_ASPECT_COLOR_BIT, getMipLevelCount(image_dudv.metaData!!))
        dySampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        dxSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        dzSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        dudvSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, getMipLevelCount(image_dudv.metaData!!).toFloat(),
                VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        normalSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, getLog2N(waterConfig.n).toFloat(),
                VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        reflectionSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, getLog2N(reflectionFbo.width).toFloat(),
                VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        refractionSampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, getLog2N(refractionFbo.width).toFloat(),
                VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        fft = FFT(deviceBundle,
                waterConfig.n, waterConfig.l, waterConfig.t_delta,
                waterConfig.amplitude, waterConfig.windDirection,
                waterConfig.windSpeed, waterConfig.capillarWavesSupression)
        normalRenderer = deviceManager.getDeviceBundle(DeviceType.MAJOR_GRAPHICS_DEVICE)?.let {
            NormalRenderer(
                    it,
                    waterConfig.n, waterConfig.normalStrength,
                    fft.dyImageView, dySampler)
        }!!
        normalRenderer.setWaitSemaphores(fft.fftSignalSemaphore.handlePointer)
        val graphicsShaderPipeline = ShaderPipeline(device.handle)
        graphicsShaderPipeline.createVertexShader("shaders/water/water.vert.spv")
        graphicsShaderPipeline.createTessellationControlShader("shaders/water/water.tesc.spv")
        graphicsShaderPipeline.createTessellationEvaluationShader("shaders/water/water.tese.spv")
        graphicsShaderPipeline.createGeometryShader("shaders/water/water.geom.spv")
        graphicsShaderPipeline.createFragmentShader("shaders/water/water.frag.spv")
        graphicsShaderPipeline.createShaderPipeline()
        val wireframeShaderPipeline = ShaderPipeline(device.handle)
        wireframeShaderPipeline.createVertexShader("shaders/water/water.vert.spv")
        wireframeShaderPipeline.createTessellationControlShader("shaders/water/water.tesc.spv")
        wireframeShaderPipeline.createTessellationEvaluationShader("shaders/water/water.tese.spv")
        wireframeShaderPipeline.createGeometryShader("shaders/water/water_wireframe.geom.spv")
        wireframeShaderPipeline.createFragmentShader("shaders/water/water_wireframe.frag.spv")
        wireframeShaderPipeline.createShaderPipeline()
        val ubo = MemoryUtil.memAlloc(java.lang.Float.BYTES * 2)
        ubo.putFloat(0f)
        ubo.putFloat(0f)
        ubo.flip()
        uniformBuffer = VkUniformBuffer(device.handle, memoryProperties, ubo)
        val descriptorSetLayout = DescriptorSetLayout(device.handle, 8)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_GEOMETRY_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_GEOMETRY_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_GEOMETRY_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(6, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(7, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT or VK10.VK_SHADER_STAGE_GEOMETRY_BIT)
        descriptorSetLayout.create()
        val descriptorSet = DescriptorSet(device.handle,
                descriptorPool!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(
                fft.dyImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                dySampler.handle, 0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                fft.dxImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                dxSampler.handle, 1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                fft.dzImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                dzSampler.handle, 2, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                imageView_dudv.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                dudvSampler.handle, 3, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                normalRenderer.normalImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                normalSampler.handle, 4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                reflectionFbo.getAttachmentImageView(FrameBufferObject.Attachment.COLOR).handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                reflectionSampler.handle, 5, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                refractionFbo.getAttachmentImageView(FrameBufferObject.Attachment.COLOR).handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                refractionSampler.handle, 6, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorBuffer(uniformBuffer.handle,
                ubo.limit().toLong(), 0, 7, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(getCamera().descriptorSet)
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(getCamera().descriptorSetLayout)
        descriptorSetLayouts.add(descriptorSetLayout)
        val vertexInput = VkVertexInput(VertexLayout.POS2D)
        val vertices = MeshGenerator.generatePatch2D4x4(128)
        val vertexBuffer = BufferUtil.createByteBuffer(vertices)
        val vertexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                vertexBuffer, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        val pushConstantsRange = java.lang.Float.BYTES * 35 + Integer.BYTES * 6 +  /*offset, since some devices casuing errors*/+java.lang.Float.BYTES
        val pushConstants = MemoryUtil.memAlloc(pushConstantsRange)
        pushConstants.put(BufferUtil.createByteBuffer(worldTransform!!.worldMatrix))
        pushConstants.putInt(waterConfig.uvScale)
        pushConstants.putInt(waterConfig.tessellationFactor)
        pushConstants.putFloat(waterConfig.tessellationSlope)
        pushConstants.putFloat(waterConfig.tessellationShift)
        pushConstants.putFloat(waterConfig.displacementScale)
        pushConstants.putInt(waterConfig.highDetailRange)
        pushConstants.putFloat(waterConfig.choppiness)
        pushConstants.putFloat(waterConfig.kReflection)
        pushConstants.putFloat(waterConfig.kRefraction)
        pushConstants.putInt(config.frameWidth)
        pushConstants.putInt(config.frameHeight)
        pushConstants.putInt(if (waterConfig.diffuse) 1 else 0)
        pushConstants.putFloat(waterConfig.emission)
        pushConstants.putFloat(waterConfig.specularFactor)
        pushConstants.putFloat(waterConfig.specularAmplifier)
        pushConstants.putFloat(waterConfig.reflectionBlendFactor)
        pushConstants.putFloat(waterConfig.baseColor.x)
        pushConstants.putFloat(waterConfig.baseColor.y)
        pushConstants.putFloat(waterConfig.baseColor.z)
        pushConstants.putFloat(waterConfig.fresnelFactor)
        pushConstants.putFloat(waterConfig.capillarStrength)
        pushConstants.putFloat(waterConfig.capillarDownsampling)
        pushConstants.putFloat(waterConfig.dudvDownsampling)
        pushConstants.putFloat(waterConfig.windDirection.x)
        pushConstants.putFloat(waterConfig.windDirection.y)
        pushConstants.flip()
        val graphicsPipeline: VkPipeline = GraphicsTessellationPipeline(device.handle,
                graphicsShaderPipeline, vertexInput, VkUtil.createLongBuffer(descriptorSetLayouts),
                config.frameWidth,
                config.frameHeight,
                resources.offScreenFbo!!.renderPass!!.handle,
                resources.offScreenFbo!!.colorAttachmentCount,
                config.multisampling_sampleCount,
                pushConstantsRange, VK10.VK_SHADER_STAGE_ALL_GRAPHICS,
                16)
        val graphicsCommandBuffer: CommandBuffer = SecondaryDrawCmdBuffer(
                device.handle, device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                graphicsPipeline.handle, graphicsPipeline.layoutHandle,
                resources.offScreenFbo!!.frameBuffer!!.handle,
                resources.offScreenFbo!!.renderPass!!.handle,
                0,
                VkUtil.createLongArray(descriptorSets),
                vertexBufferObject.handle,
                vertices.size,
                pushConstants, VK10.VK_SHADER_STAGE_ALL_GRAPHICS)
        val wireframeGraphicsPipeline: VkPipeline = GraphicsTessellationPipeline(device.handle,
                wireframeShaderPipeline, vertexInput, VkUtil.createLongBuffer(descriptorSetLayouts),
                config.frameWidth,
                config.frameHeight,
                resources.offScreenFbo!!.renderPass!!.handle,
                resources.offScreenFbo!!.colorAttachmentCount,
                config.multisampling_sampleCount,
                pushConstantsRange, VK10.VK_SHADER_STAGE_ALL_GRAPHICS,
                16)
        val wireframeCommandBuffer: CommandBuffer = SecondaryDrawCmdBuffer(
                device.handle, device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                wireframeGraphicsPipeline.handle, wireframeGraphicsPipeline.layoutHandle,
                resources.offScreenFbo!!.frameBuffer!!.handle,
                resources.offScreenFbo!!.renderPass!!.handle,
                0,
                VkUtil.createLongArray(descriptorSets),
                vertexBufferObject.handle,
                vertices.size,
                pushConstants, VK10.VK_SHADER_STAGE_ALL_GRAPHICS)
        offscreenReflectionCmdBuffer = PrimaryCmdBuffer(device.handle,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle)
        offscreenRefractionCmdBuffer = PrimaryCmdBuffer(device.handle,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle)
        reflectionFence = Fence(device.handle)
        refractionFence = Fence(device.handle)
        offScreenReflectionSubmitInfo = SubmitInfo()
        offScreenReflectionSubmitInfo.setCommandBuffers(offscreenReflectionCmdBuffer.handlePointer)
        offScreenReflectionSubmitInfo.fence = reflectionFence
        offScreenRefractionSubmitInfo = SubmitInfo()
        offScreenRefractionSubmitInfo.setCommandBuffers(offscreenRefractionCmdBuffer.handlePointer)
        offScreenRefractionSubmitInfo.fence = refractionFence
        val meshData = VkMeshData(vertexBuffer = vertexBuffer, vertexBufferObject = vertexBufferObject)
        val mainRenderInfo = VkRenderInfo(commandBuffer = graphicsCommandBuffer, descriptorSets = descriptorSets,
            descriptorSetLayouts = descriptorSetLayouts, pipeline = graphicsPipeline)
        val wireframeRenderInfo = VkRenderInfo(commandBuffer = wireframeCommandBuffer, descriptorSets = descriptorSets,
            descriptorSetLayouts = descriptorSetLayouts, pipeline = wireframeGraphicsPipeline)
        addComponent(NodeComponentType.MESH_DATA, meshData)
        addComponent(NodeComponentType.MAIN_RENDERINFO, mainRenderInfo);
	    addComponent(NodeComponentType.WIREFRAME_RENDERINFO, wireframeRenderInfo);


        // initially render to refraction fbo due to attachment deep ocean clear color
        offscreenRefractionCmdBuffer.reset()
        offscreenRefractionCmdBuffer.record(
                refractionFbo.renderPass!!.handle,
                refractionFbo.frameBuffer!!.handle,
                refractionFbo.width,
                refractionFbo.height,
                refractionFbo.colorAttachmentCount,
                refractionFbo.depthAttachmentCount,
                waterConfig.baseColor.mul(1.5f),
                null)
        offScreenRefractionSubmitInfo.submit(
                graphicsQueue)
        refractionFence.waitForFence()
    }
}