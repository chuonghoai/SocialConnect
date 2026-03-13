package com.example.frontend.presentation.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OTP_SENDED = "otp_sended_screen"
    const val FORGOT_PASSWORD_EMAIL = "forgot_password_email"
    const val FORGOT_PASSWORD_OTP_BASE = "forgot_password_otp"
    const val FORGOT_PASSWORD_OTP = "$FORGOT_PASSWORD_OTP_BASE?email={email}"
    const val FORGOT_PASSWORD_RESET_BASE = "forgot_password_reset"
    const val FORGOT_PASSWORD_RESET = "$FORGOT_PASSWORD_RESET_BASE?email={email}&otp={otp}"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val VIDEO = "video"
    const val SEARCH = "search"
    const val NOTIFICATION = "notification"
    const val CONVERSATION_LIST = "conversation_list"
    const val CHAT_BASE = "chat"
    const val CHAT = "chat/{conversationId}?name={name}&avatar={avatar}"
    const val SETTING ="setting"
    const val CREATE_POST = "create_post"
    const val POST_DETAIL = "post_detail"
}
