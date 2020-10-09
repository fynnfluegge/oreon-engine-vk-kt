package org.oreon.vk.engine;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_MEMORY_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_DEPENDENCY_BY_REGION_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R16G16B16A16_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.oreon.core.context.BaseContext;
import org.oreon.core.vk.framebuffer.FrameBufferColorAttachment;
import org.oreon.core.vk.framebuffer.FrameBufferDepthAttachment;
import org.oreon.core.vk.framebuffer.VkFrameBuffer;
import org.oreon.core.vk.framebuffer.VkFrameBufferObject;
import org.oreon.core.vk.pipeline.RenderPass;
import org.oreon.core.vk.wrapper.image.VkImageBundle;

import lombok.Getter;

@Getter
public class OffScreenFbo extends VkFrameBufferObject {

	public OffScreenFbo(VkDevice device, VkPhysicalDeviceMemoryProperties memoryProperties) {

		setWidth(BaseContext.Companion.getConfig().getFrameWidth());
		setHeight(BaseContext.Companion.getConfig().getFrameHeight());
		int samples = BaseContext.Companion.getConfig().getMultisampling_sampleCount();

		VkImageBundle albedoAttachment = new FrameBufferColorAttachment(device, memoryProperties,
				getWidth(), getHeight(), VK_FORMAT_R16G16B16A16_SFLOAT, samples);
		
		VkImageBundle worldPositionAttachment = new FrameBufferColorAttachment(device, memoryProperties,
				getWidth(), getHeight(), VK_FORMAT_R32G32B32A32_SFLOAT, samples);

		VkImageBundle normalAttachment = new FrameBufferColorAttachment(device, memoryProperties,
				getWidth(), getHeight(), VK_FORMAT_R16G16B16A16_SFLOAT, samples);

		VkImageBundle lightScatteringMaskAttachment = new FrameBufferColorAttachment(device, memoryProperties,
				getWidth(), getHeight(), VK_FORMAT_R16G16B16A16_SFLOAT, samples);
		
		VkImageBundle specularEmissionAttachment = new FrameBufferColorAttachment(device, memoryProperties,
				getWidth(), getHeight(), VK_FORMAT_R16G16B16A16_SFLOAT, samples);

		VkImageBundle depthBuffer = new FrameBufferDepthAttachment(device, memoryProperties,
				getWidth(), getHeight(), VK_FORMAT_D32_SFLOAT, samples);

		getAttachments().put(Attachment.COLOR, albedoAttachment);
		getAttachments().put(Attachment.POSITION, worldPositionAttachment);
		getAttachments().put(Attachment.NORMAL, normalAttachment);
		getAttachments().put(Attachment.LIGHT_SCATTERING, lightScatteringMaskAttachment);
		getAttachments().put(Attachment.SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM, specularEmissionAttachment);
		getAttachments().put(Attachment.DEPTH, depthBuffer);

		setRenderPass(new RenderPass(device));
		getRenderPass().addColorAttachment(0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK_IMAGE_LAYOUT_UNDEFINED,
				VK_IMAGE_LAYOUT_GENERAL);
		getRenderPass().addColorAttachment(1, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				VK_FORMAT_R32G32B32A32_SFLOAT, samples, VK_IMAGE_LAYOUT_UNDEFINED,
				VK_IMAGE_LAYOUT_GENERAL);
		getRenderPass().addColorAttachment(2, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK_IMAGE_LAYOUT_UNDEFINED,
				VK_IMAGE_LAYOUT_GENERAL);
		getRenderPass().addColorAttachment(3, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK_IMAGE_LAYOUT_UNDEFINED,
				VK_IMAGE_LAYOUT_GENERAL);
		getRenderPass().addColorAttachment(4, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK_IMAGE_LAYOUT_UNDEFINED,
				VK_IMAGE_LAYOUT_GENERAL);
		getRenderPass().addDepthAttachment(5, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
				VK_FORMAT_D32_SFLOAT, samples, VK_IMAGE_LAYOUT_UNDEFINED,
				VK_IMAGE_LAYOUT_GENERAL);

		getRenderPass().addSubpassDependency(VK_SUBPASS_EXTERNAL, 0,
				VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
				VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
				VK_ACCESS_MEMORY_READ_BIT,
				VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
				VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
				VK_DEPENDENCY_BY_REGION_BIT);
		getRenderPass().addSubpassDependency(0, VK_SUBPASS_EXTERNAL,
				VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
				VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
				VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
				VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
				VK_ACCESS_SHADER_READ_BIT,
				VK_DEPENDENCY_BY_REGION_BIT);
		getRenderPass().createSubpass();
		getRenderPass().createRenderPass();

		setDepthAttachmentCount(1);
		setColorAttachmentCount(getRenderPass().getAttachmentCount()-getDepthAttachmentCount());

		LongBuffer pImageViews = memAllocLong(getRenderPass().getAttachmentCount());
		pImageViews.put(0, getAttachments().get(Attachment.COLOR).getImageView().getHandle());
		pImageViews.put(1, getAttachments().get(Attachment.POSITION).getImageView().getHandle());
		pImageViews.put(2, getAttachments().get(Attachment.NORMAL).getImageView().getHandle());
		pImageViews.put(3, getAttachments().get(Attachment.SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM).getImageView().getHandle());
		pImageViews.put(4, getAttachments().get(Attachment.LIGHT_SCATTERING).getImageView().getHandle());
		pImageViews.put(5, getAttachments().get(Attachment.DEPTH).getImageView().getHandle());
		
		setFrameBuffer(new VkFrameBuffer(device, getWidth(), getHeight(), 1, pImageViews, getRenderPass().getHandle()));
	}

}
