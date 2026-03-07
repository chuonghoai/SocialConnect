package com.example.frontend.data.remote.api

object ApiRoutes {
    const val ME = "api/auth/me"
    const val LOGIN = "api/auth/login"
    const val REGISTER = "api/auth/register"
    const val SEND_MAIL_OTP = "api/send-otp"
    const val VERIFY_FORGOT_PASSWORD_OTP = "api/auth/forgot-password/verify-otp"
    const val RESET_PASSWORD = "api/auth/forgot-password/reset"
    const val NEWS_FEED = "api/posts/newsfeed"
    const val GET_VIDEO = "/videos"
    const val USER_POSTS = "api/posts/user/{userId}"
    const val LIKE_POST = "api/posts/{postId}/like"
}
