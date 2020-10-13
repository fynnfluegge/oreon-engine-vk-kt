package org.oreon.core.vk.util

import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.Assimp
import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import org.oreon.core.model.Mesh
import org.oreon.core.model.Model
import org.oreon.core.model.Vertex
import org.oreon.core.util.Util.toIntArray
import org.oreon.core.util.Util.toVertexArray
import java.util.*

object VkAssimpModelLoader {
    fun loadModel(path: String, file: String): List<Model> {
        var path = path
        val models: MutableList<Model> = ArrayList()
        //		List<Material<VkImage>> materials = new ArrayList<>();
        path = VkAssimpModelLoader::class.java.classLoader.getResource(path).path
        // For Linux need to keep '/' or else the Assimp.aiImportFile(...) call below returns null!
        if (System.getProperty("os.name").contains("Windows")) { // TODO Language/region agnostic value for 'Windows' ?
            if (path.startsWith("/")) path = path.substring(1)
        }
        val aiScene = Assimp.aiImportFile("$path/$file", 0)

//		if (aiScene.mMaterials() != null){
//			for (int i=0; i<aiScene.mNumMaterials(); i++){
//				AIMaterial aiMaterial = AIMaterial.create(aiScene.mMaterials().get(i));
//				Material<GLTexture> material = processMaterial(aiMaterial, path);
//				materials.add(material);
//			}
//		}
        for (i in 0 until aiScene!!.mNumMeshes()) {
            val aiMesh = AIMesh.create(aiScene.mMeshes()!![i])
            val mesh = processMesh(aiMesh)
            val model = Model()
            model.mesh = mesh
            //			int materialIndex = aiMesh.mMaterialIndex();
//			model.setMaterial(materials.get(materialIndex));
            models.add(model)
        }
        return models
    }

    private fun processMesh(aiMesh: AIMesh): Mesh {
        val vertexList: MutableList<Vertex> = ArrayList()
        val indices: MutableList<Int> = ArrayList()
        val vertices: MutableList<Vec3f> = ArrayList()
        val texCoords: MutableList<Vec2f> = ArrayList()
        val normals: MutableList<Vec3f> = ArrayList()
        val tangents: MutableList<Vec3f> = ArrayList()
        val bitangents: MutableList<Vec3f> = ArrayList()
        val aiVertices = aiMesh.mVertices()
        while (aiVertices.remaining() > 0) {
            val aiVertex = aiVertices.get()
            vertices.add(Vec3f(aiVertex.x(), aiVertex.y(), aiVertex.z()))
        }
        val aiTexCoords = aiMesh.mTextureCoords(0)
        if (aiTexCoords != null) {
            while (aiTexCoords.remaining() > 0) {
                val aiTexCoord = aiTexCoords.get()
                texCoords.add(Vec2f(aiTexCoord.x(), aiTexCoord.y()))
            }
        }
        val aiNormals = aiMesh.mNormals()
        if (aiNormals != null) {
            while (aiNormals.remaining() > 0) {
                val aiNormal = aiNormals.get()
                normals.add(Vec3f(aiNormal.x(), aiNormal.y(), aiNormal.z()))
            }
        }
        val aiTangents = aiMesh.mTangents()
        if (aiTangents != null) {
            while (aiTangents.remaining() > 0) {
                val aiTangent = aiTangents.get()
                tangents.add(Vec3f(aiTangent.x(), aiTangent.y(), aiTangent.z()))
            }
        }
        val aiBitangents = aiMesh.mBitangents()
        if (aiBitangents != null) {
            while (aiBitangents.remaining() > 0) {
                val aiBitangent = aiBitangents.get()
                bitangents.add(Vec3f(aiBitangent.x(), aiBitangent.y(), aiBitangent.z()))
            }
        }
        val aifaces = aiMesh.mFaces()
        while (aifaces.remaining() > 0) {
            val aiface = aifaces.get()
            if (aiface.mNumIndices() == 3) {
                val indicesBuffer = aiface.mIndices()
                indices.add(indicesBuffer[0])
                indices.add(indicesBuffer[1])
                indices.add(indicesBuffer[2])
            }
            if (aiface.mNumIndices() == 4) {
                val indicesBuffer = aiface.mIndices()
                indices.add(indicesBuffer[0])
                indices.add(indicesBuffer[1])
                indices.add(indicesBuffer[2])
                indices.add(indicesBuffer[0])
                indices.add(indicesBuffer[1])
                indices.add(indicesBuffer[3])
                indices.add(indicesBuffer[1])
                indices.add(indicesBuffer[2])
                indices.add(indicesBuffer[3])
            }
        }
        for (i in vertices.indices) {
            val vertex = Vertex()
            vertex.position = vertices[i]
            if (!normals.isEmpty()) {
                vertex.normal = normals[i]
            } else {
                vertex.normal = Vec3f(0f, 0f, 0f)
            }
            if (!texCoords.isEmpty()) {
                vertex.uvCoord = texCoords[i]
            } else {
                vertex.uvCoord = Vec2f(0f, 0f)
            }
            if (!tangents.isEmpty()) {
                vertex.tangent = tangents[i]
            }
            if (!bitangents.isEmpty()) {
                vertex.bitangent = bitangents[i]
            }
            vertexList.add(vertex)
        }
        val vertexData: Array<Vertex> = toVertexArray(vertexList)
        val facesData = toIntArray(indices)
        return Mesh(vertexData, facesData)
    }
    //	private static Material<GLTexture> processMaterial(AIMaterial aiMaterial, String texturesDir) {
    //
    //	    AIString path = AIString.calloc();
    //	    Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
    //	    String textPath = path.dataString();
    //
    //	    GLTexture diffuseTexture = null;
    //	    if (textPath != null && textPath.length() > 0) {
    //	    	diffuseTexture = new Texture2DTrilinearFilter(texturesDir + "/" + textPath);
    //	    }
    //
    //	    AIColor4D color = AIColor4D.create();
    //	    Vec3f diffuseColor = null;
    //	    int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT, Assimp.aiTextureType_NONE, 0, color);
    //	    if (result == 0) {
    //	    	diffuseColor = new Vec3f(color.r(), color.g(), color.b());
    //	    }
    //
    //	    Material<GLTexture> material = new Material<GLTexture>();
    //	    material.setDiffusemap(diffuseTexture);
    //	    material.setColor(diffuseColor);
    //	    
    //	    return material;
    //	}
}