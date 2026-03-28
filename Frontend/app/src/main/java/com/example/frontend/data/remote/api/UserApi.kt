package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.AddFriendRequestDto
import com.example.frontend.data.remote.dto.FriendListItem
import com.example.frontend.data.remote.dto.FriendRequestDto
import com.example.frontend.data.remote.dto.FriendListResponseDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

interface UserApi {
    @POST(ApiRoutes.ADD_FRIEND)
    suspend fun addFriend(@Body body: AddFriendRequestDto): Response<Unit>

    @GET(ApiRoutes.GET_FRIEND_REQUESTS)
    suspend fun getFriendRequests(): Response<List<FriendRequestDto>>

    @PATCH(ApiRoutes.ACCEPT_FRIEND_REQUEST)
    suspend fun acceptFriendRequest(@Path("friendId") friendId: String): Response<Unit>

    @DELETE(ApiRoutes.REJECT_FRIEND_REQUEST)
    suspend fun rejectFriendRequest(@Path("friendId") friendId: String): Response<Unit>

    @GET(ApiRoutes.GET_MY_FRIENDS)
    suspend fun getMyFriends(): Response<FriendListResponseDto>

    @GET(ApiRoutes.GET_USER_FRIENDS)
    suspend fun getUserFriends(@Path("userId") userId: String): Response<FriendListResponseDto>

}
