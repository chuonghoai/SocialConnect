package com.example.frontend.data.remote.api

import ChangePasswordRequest
import UpdateProfileRequest
import android.R
import com.example.frontend.domain.model.Token
import com.example.frontend.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.PUT

interface AuthApi {
    @GET(ApiRoutes.ME)
    suspend fun me(): User

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

    @PUT(ApiRoutes.UPDATE_PROFILE)
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @PUT(ApiRoutes.CHANGE_PASSWORD)
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<User>

    @POST(ApiRoutes.LOGIN)
    suspend fun logout(): Map<String, String>
}