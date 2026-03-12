package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface MediaApi {
    @GET(ApiRoutes.GET_SIGNATURE)
    suspend fun getSignature(): SignatureBaseResponse

    @Multipart
    @POST
    suspend fun uploadToCloudinary(
        @Url url: String,
        @Part file: MultipartBody.Part,
        @Part("api_key") apiKey: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("signature") signature: RequestBody,
        @Part("folder") folder: RequestBody,
        @Part("tags") tags: RequestBody
    ): CloudinaryResponseDto

    @POST(ApiRoutes.SAVE_MEDIA)
    suspend fun saveMedia(@Body request: SaveMediaRequestDto): SaveMediaBaseResponse
}