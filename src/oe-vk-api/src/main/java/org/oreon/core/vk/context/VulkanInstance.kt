package org.oreon.core.vk.context

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.util.VkUtil

class VulkanInstance(ppEnabledLayerNames: PointerBuffer?) {
    val handle: VkInstance
    private val debugCallbackHandle: Long
    private fun setupDebugging(instance: VkInstance, flags: Int, callback: VkDebugReportCallbackEXT): Long {
        val debugCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
                .sType(EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(0)
                .pfnCallback(callback)
                .pUserData(0)
                .flags(flags)
        val pCallback = MemoryUtil.memAllocLong(1)
        val err = EXTDebugReport.vkCreateDebugReportCallbackEXT(instance, debugCreateInfo, null, pCallback)
        val callbackHandle = pCallback[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create VkInstance: " + VkUtil.translateVulkanResult(err))
        }
        MemoryUtil.memFree(pCallback)
        debugCreateInfo.free()
        return callbackHandle
    }

    fun destroy() {
        EXTDebugReport.vkDestroyDebugReportCallbackEXT(handle, debugCallbackHandle, null)
        VK10.vkDestroyInstance(handle, null)
    }

    init {
        val requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()
                ?: throw AssertionError("Failed to find list of required Vulkan extensions")
        val VK_EXT_DEBUG_REPORT_EXTENSION = MemoryUtil.memUTF8(EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME)

        // +1 due to VK_EXT_DEBUG_REPORT_EXTENSION
        val ppEnabledExtensionNames = MemoryUtil.memAllocPointer(requiredExtensions.remaining() + 1)
        ppEnabledExtensionNames.put(requiredExtensions)
        ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION)
        ppEnabledExtensionNames.flip()
        val appInfo = VkApplicationInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(MemoryUtil.memUTF8("Vulkan Demo"))
                .pEngineName(MemoryUtil.memUTF8("OREON ENGINE"))
                .apiVersion(VK10.VK_MAKE_VERSION(1, 1, 77))
        val pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(0)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames)
                .ppEnabledLayerNames(ppEnabledLayerNames)
        val pInstance = MemoryUtil.memAllocPointer(1)
        val err = VK10.vkCreateInstance(pCreateInfo, null, pInstance)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create VkInstance: " + VkUtil.translateVulkanResult(err))
        }
        handle = VkInstance(pInstance[0], pCreateInfo)
        pCreateInfo.free()
        MemoryUtil.memFree(pInstance)
        MemoryUtil.memFree(VK_EXT_DEBUG_REPORT_EXTENSION)
        MemoryUtil.memFree(ppEnabledExtensionNames)
        MemoryUtil.memFree(appInfo.pApplicationName())
        MemoryUtil.memFree(appInfo.pEngineName())
        appInfo.free()
        val debugCallback: VkDebugReportCallbackEXT = object : VkDebugReportCallbackEXT() {
            override fun invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long): Int {
                System.err.println("ERROR OCCURED: " + getString(pMessage))
                return 0
            }
        }
        debugCallbackHandle = setupDebugging(handle, EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT or EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback)
    }
}