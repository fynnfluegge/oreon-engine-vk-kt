package org.oreon.vk.engine

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.core.RenderEngine
import org.oreon.core.context.BaseContext
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.scenegraph.RenderList
import org.oreon.core.target.FrameBufferObject
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.resources
import org.oreon.core.vk.context.VkContext.surface
import org.oreon.core.vk.context.VkContext.vkInstance
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.swapchain.SwapChain
import org.oreon.core.vk.synchronization.VkSemaphore
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.command.PrimaryCmdBuffer
import org.oreon.vk.components.atmosphere.VkDirectionalLight
import org.oreon.vk.components.filter.Bloom
import org.oreon.vk.components.planet.Planet
import org.oreon.vk.components.ui.VkGUI
import java.util.*
import java.util.function.Consumer

class VkDeferredEngine : RenderEngine() {
    private var swapChain: SwapChain? = null
    private var graphicsDevice: VkDeviceBundle? = null
    private var offScreenFbo: VkFrameBufferObject? = null
    private var transparencyFbo: VkFrameBufferObject? = null
    private var offScreenSemaphore: VkSemaphore? = null
    private var deferredStageSemaphore: VkSemaphore? = null
    private var transparencySemaphore: VkSemaphore? = null
    private var postProcessingSemaphore: VkSemaphore? = null
    private var deferredStageCmdBuffer: CommandBuffer? = null
    private var deferredStageSubmitInfo: SubmitInfo? = null
    private var postProcessingCmdBuffer: CommandBuffer? = null
    private var postProcessingSubmitInfo: SubmitInfo? = null
    private var offScreenPrimaryCmdBuffer: PrimaryCmdBuffer? = null
    private var offScreenSecondaryCmdBuffers: LinkedHashMap<String, CommandBuffer>? = null
    private var offScreenRenderList: RenderList? = null
    private var offScreenSubmitInfo: SubmitInfo? = null
    private var transparencyPrimaryCmdBuffer: PrimaryCmdBuffer? = null
    private var transparencySecondaryCmdBuffers: LinkedHashMap<String, CommandBuffer>? = null
    private var transparencyRenderList: RenderList? = null
    private var transparencySubmitInfo: SubmitInfo? = null

    // uniform buffers
    // private VkUniformBuffer renderStateUbo;
    private var sampleCoverage: SampleCoverage? = null
    private var deferredLighting: DeferredLighting? = null
    private var fxaa: FXAA? = null
    private var opaqueTransparencyBlending: OpaqueTransparencyBlending? = null

    // post processing filter
    private var bloom: Bloom? = null

    // gui
    var gui: VkGUI? = null

    override fun init() {
        super.init()
        sceneGraph.addObject(VkDirectionalLight())
        offScreenRenderList = RenderList()
        transparencyRenderList = RenderList()
        offScreenSecondaryCmdBuffers = LinkedHashMap()
        transparencySecondaryCmdBuffers = LinkedHashMap()
        graphicsDevice = deviceManager.getDeviceBundle(DeviceType.MAJOR_GRAPHICS_DEVICE)
        offScreenFbo = OffScreenFbo(graphicsDevice!!.logicalDevice.handle,
                graphicsDevice!!.physicalDevice.memoryProperties)
        transparencyFbo = TransparencyFbo(graphicsDevice!!.logicalDevice.handle,
                graphicsDevice!!.physicalDevice.memoryProperties)
        resources.offScreenFbo = offScreenFbo
        resources.transparencyFbo = transparencyFbo

        // Semaphore creations
        offScreenSemaphore = VkSemaphore(graphicsDevice!!.logicalDevice.handle)
        deferredStageSemaphore = VkSemaphore(graphicsDevice!!.logicalDevice.handle)
        transparencySemaphore = VkSemaphore(graphicsDevice!!.logicalDevice.handle)
        postProcessingSemaphore = VkSemaphore(graphicsDevice!!.logicalDevice.handle)

        // offscreen opaque primary command buffer creation
        offScreenPrimaryCmdBuffer = PrimaryCmdBuffer(graphicsDevice!!.logicalDevice.handle,
                graphicsDevice!!.logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)!!.handle)
        offScreenSubmitInfo = SubmitInfo()
        offScreenSubmitInfo!!.setCommandBuffers(offScreenPrimaryCmdBuffer!!.handlePointer)
        offScreenSubmitInfo!!.setSignalSemaphores(offScreenSemaphore!!.handlePointer)

        // offscreen transparency primary command buffer creation
        transparencyPrimaryCmdBuffer = PrimaryCmdBuffer(graphicsDevice!!.logicalDevice.handle,
                graphicsDevice!!.logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)!!.handle)
        transparencySubmitInfo = SubmitInfo()
        transparencySubmitInfo!!.setCommandBuffers(transparencyPrimaryCmdBuffer!!.handlePointer)
        transparencySubmitInfo!!.setSignalSemaphores(transparencySemaphore!!.handlePointer)
        sampleCoverage = SampleCoverage(graphicsDevice!!,
                BaseContext.config.frameWidth,
                BaseContext.config.frameHeight,
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.POSITION),
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.LIGHT_SCATTERING),
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM))
        deferredLighting = DeferredLighting(graphicsDevice!!,
                BaseContext.config.frameWidth,
                BaseContext.config.frameHeight,
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.COLOR),
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.POSITION),
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.NORMAL),
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM),
                sampleCoverage!!.sampleCoverageImageView)
        val opaqueTransparencyBlendWaitSemaphores = MemoryUtil.memAllocLong(2)
        opaqueTransparencyBlendWaitSemaphores.put(0, deferredStageSemaphore!!.handle)
        opaqueTransparencyBlendWaitSemaphores.put(1, transparencySemaphore!!.handle)
        opaqueTransparencyBlending = OpaqueTransparencyBlending(graphicsDevice!!,
                BaseContext.config.frameWidth,
                BaseContext.config.frameHeight,
                deferredLighting!!.deferredLightingSceneImageView,
                (offScreenFbo as OffScreenFbo).getAttachmentImageView(FrameBufferObject.Attachment.DEPTH),
                sampleCoverage!!.lightScatteringImageView,
                (transparencyFbo as TransparencyFbo).getAttachmentImageView(FrameBufferObject.Attachment.COLOR),
                (transparencyFbo as TransparencyFbo).getAttachmentImageView(FrameBufferObject.Attachment.DEPTH),
                (transparencyFbo as TransparencyFbo).getAttachmentImageView(FrameBufferObject.Attachment.ALPHA),
                (transparencyFbo as TransparencyFbo).getAttachmentImageView(FrameBufferObject.Attachment.LIGHT_SCATTERING),
                opaqueTransparencyBlendWaitSemaphores)
        var displayImageView = deferredLighting!!.deferredLightingSceneImageView
        if (BaseContext.config.fxaaEnabled) {
            fxaa = FXAA(graphicsDevice!!,
                    BaseContext.config.frameWidth,
                    BaseContext.config.frameHeight,
                    displayImageView)
            displayImageView = fxaa!!.fxaaImageView
        }
        if (BaseContext.config.bloomEnabled) {
            bloom = Bloom(graphicsDevice!!,
                    BaseContext.config.frameWidth,
                    BaseContext.config.frameHeight,
                    displayImageView,
                    sampleCoverage!!.specularEmissionDiffuseSsaoBloomImageView)
            displayImageView = bloom!!.bloomSceneImageBundle.imageView
        }
        if (gui != null) {
            // all post procssing effects and FXAA disabled
            gui!!.init(displayImageView, postProcessingSemaphore!!.handlePointer)
            displayImageView = gui!!.imageView
        }
        swapChain = SwapChain(graphicsDevice!!.logicalDevice, graphicsDevice!!.physicalDevice,
                surface, displayImageView.handle)

        // record sample coverage + deferred lighting command buffer
        deferredStageCmdBuffer = CommandBuffer(graphicsDevice!!.logicalDevice.handle,
                graphicsDevice!!.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        deferredStageCmdBuffer!!.beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        sampleCoverage!!.record(deferredStageCmdBuffer!!)
        deferredStageCmdBuffer!!.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        deferredLighting!!.record(deferredStageCmdBuffer!!)
        deferredStageCmdBuffer!!.finishRecord()
        val pComputeShaderWaitDstStageMask = MemoryUtil.memAllocInt(1)
        pComputeShaderWaitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        deferredStageSubmitInfo = SubmitInfo(deferredStageCmdBuffer!!.handlePointer)
        deferredStageSubmitInfo!!.setWaitSemaphores(offScreenSemaphore!!.handlePointer)
        deferredStageSubmitInfo!!.setWaitDstStageMask(pComputeShaderWaitDstStageMask)
        deferredStageSubmitInfo!!.setSignalSemaphores(deferredStageSemaphore!!.handlePointer)

        // record post processing command buffer
        postProcessingCmdBuffer = CommandBuffer(graphicsDevice!!.logicalDevice.handle,
                graphicsDevice!!.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        postProcessingCmdBuffer!!.beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        if (BaseContext.config.fxaaEnabled) {
            fxaa!!.record(postProcessingCmdBuffer!!)
        }
        postProcessingCmdBuffer!!.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        if (BaseContext.config.bloomEnabled) {
            bloom!!.record(postProcessingCmdBuffer!!)
        }
        postProcessingCmdBuffer!!.finishRecord()
        postProcessingSubmitInfo = SubmitInfo(postProcessingCmdBuffer!!.handlePointer)
        postProcessingSubmitInfo!!.setWaitSemaphores(opaqueTransparencyBlending!!.signalSemaphore.handlePointer)
        postProcessingSubmitInfo!!.setWaitSemaphores(deferredStageSemaphore!!.handlePointer)
        postProcessingSubmitInfo!!.setWaitDstStageMask(pComputeShaderWaitDstStageMask)
        postProcessingSubmitInfo!!.setSignalSemaphores(postProcessingSemaphore!!.handlePointer)
    }

    override fun render() {
        sceneGraph.render()
        offScreenRenderList!!.changed = false
        sceneGraph.record(offScreenRenderList)

        // update Terrain/Planet Quadtree
        if (sceneGraph.hasTerrain()) {
            if (camera.isCameraMoved) {
                // start waiting updateQuadtree thread
                (sceneGraph.terrain as Planet).quadtree.signal()
            }
        }

        // record new primary Command Buffer if renderList has changed
        if (offScreenRenderList!!.hasChanged()) {
            offScreenSecondaryCmdBuffers!!.clear()
            offScreenRenderList!!.keySet.forEach(Consumer { key: String ->
                if (!offScreenSecondaryCmdBuffers!!.containsKey(key)) {
                    val mainRenderInfo: VkRenderInfo? = offScreenRenderList!![key]
                            ?.getComponent(NodeComponentType.MAIN_RENDERINFO)
                    if (mainRenderInfo != null) offScreenSecondaryCmdBuffers!![key] = mainRenderInfo.commandBuffer!!
                }
            })

            // Offscreen primary render command buffer
            offScreenPrimaryCmdBuffer!!.reset()
            offScreenPrimaryCmdBuffer!!.record(offScreenFbo!!.renderPass!!.handle,
                    offScreenFbo!!.frameBuffer!!.handle,
                    offScreenFbo!!.width,
                    offScreenFbo!!.height,
                    offScreenFbo!!.colorAttachmentCount,
                    offScreenFbo!!.depthAttachmentCount,
                    VkUtil.createPointerBuffer(offScreenSecondaryCmdBuffers!!.values))
        }
        if (!offScreenRenderList!!.isEmpty) {
            offScreenSubmitInfo!!.submit(graphicsDevice!!.logicalDevice.graphicsQueue)
        }
        deferredStageSubmitInfo!!.submit(graphicsDevice!!.logicalDevice.computeQueue)
        transparencyRenderList!!.changed = false
        sceneGraph.recordTransparentObjects(transparencyRenderList)
        if (transparencyRenderList!!.hasChanged() && !transparencyRenderList!!.isEmpty) {
            transparencySecondaryCmdBuffers!!.clear()
            transparencyRenderList!!.keySet.forEach(Consumer { key: String ->
                if (!transparencySecondaryCmdBuffers!!.containsKey(key)) {
                    val mainRenderInfo: VkRenderInfo? = transparencyRenderList!![key]
                            ?.getComponent(NodeComponentType.MAIN_RENDERINFO)
                    transparencySecondaryCmdBuffers!![key] = mainRenderInfo!!.commandBuffer!!
                }
            })

            // Tranparency primary render command buffer
            transparencyPrimaryCmdBuffer!!.reset()
            transparencyPrimaryCmdBuffer!!.record(transparencyFbo!!.renderPass!!.handle,
                    transparencyFbo!!.frameBuffer!!.handle,
                    transparencyFbo!!.width,
                    transparencyFbo!!.height,
                    transparencyFbo!!.colorAttachmentCount,
                    transparencyFbo!!.depthAttachmentCount,
                    VkUtil.createPointerBuffer(transparencySecondaryCmdBuffers!!.values))
        }
        if (!transparencyRenderList!!.isEmpty) {
            transparencySubmitInfo!!.submit(graphicsDevice!!.logicalDevice.graphicsQueue)
            opaqueTransparencyBlending!!.render()
        }
        postProcessingSubmitInfo!!.submit(graphicsDevice!!.logicalDevice.computeQueue)
        gui?.render()
        swapChain!!.draw(graphicsDevice!!.logicalDevice.graphicsQueue,
                if (gui != null) gui!!.signalSemaphore else postProcessingSemaphore)
        swapChain!!.drawFence.waitForFence()
    }

    override fun update() {
        super.update()
        gui?.update()
    }

    override fun shutdown() {

        // wait for queues to be finished before destroy vulkan objects
        VK10.vkDeviceWaitIdle(graphicsDevice!!.logicalDevice.handle)
        super.shutdown()
        offScreenFbo!!.destroy()
        transparencyFbo!!.destroy()
        offScreenSemaphore!!.destroy()
        deferredStageSemaphore!!.destroy()
        transparencySemaphore!!.destroy()
        postProcessingSemaphore!!.destroy()
        offScreenPrimaryCmdBuffer!!.destroy()
        deferredStageCmdBuffer!!.destroy()
        postProcessingCmdBuffer!!.destroy()
        transparencyPrimaryCmdBuffer!!.destroy()
        sampleCoverage!!.shutdown()
        deferredLighting!!.shutdown()
        if (fxaa != null) {
            fxaa!!.shutdown()
        }
        opaqueTransparencyBlending!!.shutdown()
        if (bloom != null) {
            bloom!!.shutdown()
        }
        gui?.shutdown()
        swapChain!!.destroy()
        BaseContext.camera.shutdown()
        graphicsDevice!!.logicalDevice.destroy()
        vkInstance.destroy()
    }
}