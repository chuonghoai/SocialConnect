package com.example.frontend.data.remote.api

object ApiRoutes {
    const val ME = "api/auth/me"
    const val USER_PROFILE = "api/users/{userId}"
    const val LOGIN = "api/auth/login"
    const val LOGOUT = "api/auth/logout"
    const val REGISTER = "api/auth/register"
    const val SEND_MAIL_OTP = "api/auth/send-otp"
    const val NEWS_FEED = "api/posts/newsfeed"
    const val GET_VIDEO = "videos"
    const val USER_POSTS = "api/posts/user/{userId}"
    const val SAVED_POSTS = "api/posts/saved"
    const val SHARE_POST = "api/posts/{postId}/share"
    const val LIKE_POST = "api/posts/{postId}/like"
    const val SAVE_POST = "api/posts/{postId}/save"
    const val UPDATE_POST = "api/posts/{postId}"
    const val DELETE_POST = "api/posts/{postId}"
    const val LIKE_VIDEO = "api/videos/{videoId}/like"
    const val CREATE_VIDEO_COMMENT = "api/videos/{videoId}/comments"
    const val SHARE_VIDEO = "api/videos/{videoId}/share"
    const val SAVE_VIDEO = "api/videos/{videoId}/save"
    const val GET_POST_COMMENTS = "api/posts/{postId}/comments"
    const val CREATE_POST_COMMENT = "api/posts/{postId}/comments"
    const val VERIFY_FORGOT_PASSWORD_OTP = "api/auth/forgot-password/verify-otp"
    const val RESET_PASSWORD = "api/auth/forgot-password/reset"
    const val SEARCH = "api/search"
    const val GET_SIGNATURE = "api/media/signature"
    const val SAVE_MEDIA = "api/media"
    const val CREATE_POST = "api/posts"
    const val GET_POST_BY_ID = "api/posts/{id}"
    const val UPDATE_PROFILE = "/api/auth/me"
    const val CHANGE_PASSWORD = "/auth/change-password"
    const val GET_NOTIFICATIONS_ME = "api/notifications/me"
    const val MARK_NOTIFICATION_READ = "api/notifications/{notificationId}/read"
    const val GET_MY_FRIENDS = "api/users/me/friends"
    const val ADD_FRIEND = "api/users/me/friends"
    const val GET_FRIEND_REQUESTS = "api/users/me/friends/requests"
    const val ACCEPT_FRIEND_REQUEST = "api/users/me/friends/requests/{friendId}/accept"
    const val REJECT_FRIEND_REQUEST = "api/users/me/friends/requests/{friendId}/reject"
    const val GET_USER_FRIENDS = "api/users/{userId}/friends"




    // Conversation
    const val CONVERSATIONS = "api/conversations"
    const val MESSAGES = "api/messages/{conversationId}"
}
