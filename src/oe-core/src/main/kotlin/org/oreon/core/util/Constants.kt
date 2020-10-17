package org.oreon.core.util

import org.oreon.core.math.Vec4f

object Constants {
    const val NANOSECOND = 1000000000L
    const val ZFAR = 10000.0f
    const val ZNEAR = 0.1f
    var ZEROPLANE = Vec4f(0f, 0f, 0f, 0f)
    var CLIPOFFSET = 0
    const val PSSM_SPLITS = 5
    val PSSM_SPLIT_SHEME = floatArrayOf(-0.02f, 0.02f,
            -0.04f, 0.04f,
            -0.1f, 0.1f,
            -0.5f, 0.5f,
            -1f, 1f)

    // Global Uniform Block Bindings
    const val CameraUniformBlockBinding = 51
    const val DirectionalLightUniformBlockBinding = 52
    const val LightMatricesUniformBlockBinding = 53 //example spotlight
    /*PointLight light = new PointLight(getTransform().getTranslation(), new Vec3f(1,1,1), new Vec3f(1.0f,1.0f,0.8f), 1f);
	light.getLocalTransform().getTranslation().setY(-10);
	if (i == 1)
		light.setEnabled(1);
	light.setSpot(1);
	light.setConstantAttenuation(0.01f);
	light.setLinearAttenuation(0.005f);
	light.setQuadraticAttenuation(0.00005f);
	light.setConeDirection(new Vec3f(0,-1,0));
	light.setSpotCosCutoff(0.8f);
	light.setSpotExponent(20);
	drone.addComponent("Light", light);*/
}