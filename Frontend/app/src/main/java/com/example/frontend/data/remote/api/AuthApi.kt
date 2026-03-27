package com.example.frontend.data.remote.api

import ChangePasswordRequest
import UpdateProfileRequest
import com.example.frontend.domain.model.Token
import com.example.frontend.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.PATCH

interface AuthApi {
    @GET(ApiRoutes.ME)
    suspend fun me(): User

    @GET(ApiRoutes.USER_PROFILE)
    suspend fun getUserProfile(@Path("userId") userId: String): User

    @POST(ApiRoutes.LOGIN)
    suspend fun login(@Body req: Map<String, String>): Token

    @POST(ApiRoutes.REGISTER)
    suspend fun register(@Body req: Map<String, String>): Map<String, String>

    @POST(ApiRoutes.SEND_MAIL_OTP)
    suspend fun sendOtp(@Body req: Map<String, String>): Map<String, String>

    @POST(ApiRoutes.VERIFY_FORGOT_PASSWORD_OTP)
    suspend fun verifyForgotPasswordOtp(@Body req: Map<String, String>): Map<String, String>

    @POST(ApiRoutes.RESET_PASSWORD)
    suspend fun resetPassword(@Body req: Map<String, String>): Map<String, String>

    @PATCH(ApiRoutes.UPDATE_PROFILE)
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @PATCH(ApiRoutes.CHANGE_PASSWORD)
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Map<String, String>>

    @POST(ApiRoutes.LOGOUT)
    suspend fun logout(): Map<String, String>
}
