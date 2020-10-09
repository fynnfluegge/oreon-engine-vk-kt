package org.oreon.core.vk.pipeline

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.util.VkUtil
import java.util.*

class RenderPass(private val device: VkDevice) {
    private val colorReferences: MutableList<VkAttachmentReference> = ArrayList()
    private var depthReference: VkAttachmentReference? = null
    private val attachmentDescriptions: MutableList<VkAttachmentDescription> = ArrayList()
    private val subpassDependendies: MutableList<VkSubpassDependency> = ArrayList()
    private val subpassDescriptions: MutableList<VkSubpassDescription> = ArrayList()

    var handle: Long = 0

    var attachmentCount = 0

    fun createRenderPass() {
        val attachments = VkAttachmentDescription.calloc(attachmentDescriptions.size)
        for (attachment in attachmentDescriptions) {
            attachments.put(attachment)
        }
        attachments.flip()
        val subpasses = VkSubpassDescription.calloc(subpassDescriptions.size)
        for (subpass in subpassDescriptions) {
            subpasses.put(subpass)
        }
        subpasses.flip()
        val dependencies = VkSubpassDependency.calloc(subpassDependendies.size)
        for (dependency in subpassDependendies) {
            dependencies.put(dependency)
        }
        dependencies.flip()
        val renderPassInfo = VkRenderPassCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pNext(0)
                .pAttachments(attachments)
                .pSubpasses(subpasses)
                .pDependencies(dependencies)
        val pRenderPass = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateRenderPass(device, renderPassInfo, null, pRenderPass)
        handle = pRenderPass[0]
        for (attachment in attachmentDescriptions) {
            attachment.free()
        }
        for (subpass in subpassDescriptions) {
            subpass.free()
        }
        for (dependency in subpassDependendies) {
            dependency.free()
        }
        attachmentCount = attachments.limit()
        MemoryUtil.memFree(pRenderPass)
        renderPassInfo.free()
        subpasses.free()
        dependencies.free()
        attachments.free()
        if (depthReference != null) {
            depthReference!!.free()
        }
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create render pass: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun addColorAttachment(location: Int, layout: Int, format: Int,
                           samples: Int, initialLayout: Int, finalLayout: Int) {
        addAttachmentDescription(format, samples, initialLayout, finalLayout)
        addColorAttachmentReference(location, layout)
    }

    fun addDepthAttachment(location: Int, layout: Int, format: Int,
                           samples: Int, initialLayout: Int, finalLayout: Int) {
        addAttachmentDescription(format, samples, initialLayout, finalLayout)
        addDepthAttachmentReference(location, layout)
    }

    fun addSubpassDependency(srcSubpass: Int, dstSubpass: Int,
                             srcStageMask: Int, dstStageMask: Int, srcAccessMask: Int,
                             dstAccessMask: Int, dependencyFlags: Int) {
        val dependencies = VkSubpassDependency.calloc()
                .srcSubpass(srcSubpass)
                .dstSubpass(dstSubpass)
                .srcStageMask(srcStageMask)
                .dstStageMask(dstStageMask)
                .srcAccessMask(srcStageMask)
                .dstAccessMask(dstStageMask)
                .dependencyFlags(dependencyFlags)
        subpassDependendies.add(dependencies)
    }

    private fun addColorAttachmentReference(location: Int, layout: Int) {
        val attachmentReference = VkAttachmentReference.calloc()
                .attachment(location)
                .layout(layout)
        colorReferences.add(attachmentReference)
    }

    private fun addDepthAttachmentReference(location: Int, layout: Int) {
        depthReference = VkAttachmentReference.calloc()
                .attachment(location)
                .layout(layout)
    }

    private fun addAttachmentDescription(format: Int, samples: Int,
                                         initialLayout: Int, finalLayout: Int) {
        val attachment = VkAttachmentDescription.calloc()
                .format(format)
                .samples(VkUtil.getSampleCountBit(samples))
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(initialLayout)
                .finalLayout(finalLayout)
        attachmentDescriptions.add(attachment)
    }

    fun createSubpass() {
        val attachmentReferenceBuffer = VkAttachmentReference.calloc(colorReferences.size)
        for (reference in colorReferences) {
            attachmentReferenceBuffer.put(reference)
        }
        attachmentReferenceBuffer.flip()
        val subpass = VkSubpassDescription.calloc()
                .pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .flags(0)
                .pInputAttachments(null)
                .colorAttachmentCount(attachmentReferenceBuffer.limit())
                .pColorAttachments(attachmentReferenceBuffer)
                .pResolveAttachments(null)
                .pDepthStencilAttachment(depthReference)
                .pPreserveAttachments(null)
        for (reference in colorReferences) {
            reference.free()
        }
        colorReferences.clear()
        subpassDescriptions.add(subpass)
    }

    fun destroy() {
        VK10.vkDestroyRenderPass(device, handle, null)
    }
}