package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.LoginRequestDto
import com.example.frontend.data.remote.dto.LoginResponseDto
import com.example.frontend.data.remote.dto.MeResponseDto
import com.example.frontend.data.remote.dto.RegisterRequestDto
import com.example.frontend.data.remote.dto.sendOtpRequestDto
import com.example.frontend.data.remote.dto.sendOtpResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @GET(ApiRoutes.ME)
    suspend fun me(): MeResponseDto

    @POST(ApiRoutes.LOGIN)
    suspend fun login(@Body req: LoginRequestDto): LoginResponseDto

    @POST(ApiRoutes.REGISTER)
    suspend fun register(@Body req: RegisterRequestDto): LoginResponseDto

    @POST(ApiRoutes.SEND_MAIL_OTP)
    suspend fun sendMailOtp(@Body req: sendOtpRequestDto): sendOtpResponseDto
}
