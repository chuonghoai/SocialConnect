package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.ConversationApi
import com.example.frontend.data.remote.api.UserApi
import com.example.frontend.data.remote.dto.AddFriendRequestDto
import com.example.frontend.domain.model.FriendRequestItem
import com.example.frontend.domain.model.FriendRecipient
import com.example.frontend.domain.repository.FriendRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi,
    private val userApi: UserApi
) : FriendRepository {

    override suspend fun getMyFriends(): ApiResult<List<FriendRecipient>> {
        return try {
            val response = userApi.getMyFriends()
            if (response.isSuccessful) {
                val friends = response.body()?.friends.orEmpty().map { dto ->
                    val displayName = dto.displayName
                        .takeIf { it.isNotBlank() }
                        ?: dto.username.takeIf { it.isNotBlank() }
                        ?: "Người dùng"

                    FriendRecipient(
                        id = dto.id,
                        displayName = displayName,
                        username = dto.username,
                        avatarUrl = dto.avatarUrl,
                        isOnline = dto.isOnline
                    )
                }
                ApiResult.Success(friends)
            } else {
                ApiResult.Error(
                    code = response.code(),
                    message = "Không thể tải danh sách bạn bè (${response.code()})"
                )
            }
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "Không thể tải danh sách bạn bè (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Lỗi mạng: không thể tải danh sách bạn bè",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Không thể tải danh sách bạn bè",
                throwable = e
            )
        }
    }

    override suspend fun getFriendRequests(): ApiResult<List<FriendRequestItem>> {
        return try {
            val response = userApi.getFriendRequests()
            if (!response.isSuccessful) {
                return ApiResult.Error(
                    code = response.code(),
                    message = "Khong the tai loi moi ket ban (${response.code()})"
                )
            }

            val requests = response.body().orEmpty().map { dto ->
                val fromDisplayName = dto.user1.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: dto.user1.username?.takeIf { it.isNotBlank() }
                    ?: "Nguoi dung"

                FriendRequestItem(
                    requestId = dto.id,
                    fromUserId = dto.user1.id,
                    fromDisplayName = fromDisplayName,
                    fromUsername = dto.user1.username.orEmpty(),
                    fromAvatarUrl = dto.user1.avatarUrl
                )
            }

            ApiResult.Success(requests)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "Khong the tai loi moi ket ban (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Loi mang: khong the tai loi moi ket ban",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Khong the tai loi moi ket ban",
                throwable = e
            )
        }
    }

    override suspend fun acceptFriendRequest(friendId: String): ApiResult<Unit> {
        return try {
            val response = userApi.acceptFriendRequest(friendId)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(
                    code = response.code(),
                    message = "Khong the chap nhan loi moi (${response.code()})"
                )
            }
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "Khong the chap nhan loi moi (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Loi mang: khong the chap nhan loi moi",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Khong the chap nhan loi moi",
                throwable = e
            )
        }
    }

    override suspend fun rejectFriendRequest(friendId: String): ApiResult<Unit> {
        return try {
            val response = userApi.rejectFriendRequest(friendId)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(
                    code = response.code(),
                    message = "Khong the tu choi loi moi (${response.code()})"
                )
            }
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "Khong the tu choi loi moi (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Loi mang: khong the tu choi loi moi",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Khong the tu choi loi moi",
                throwable = e
            )
        }
    }

    override suspend fun addFriend(friendId: String): ApiResult<Unit> {
        if (friendId.isBlank()) {
            return ApiResult.Error(message = "Thiếu friendId")
        }

        return try {
            val response = userApi.addFriend(AddFriendRequestDto(friendId = friendId))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                val serverMessage = extractServerMessage(errorBody)
                ApiResult.Error(
                    code = response.code(),
                    message = serverMessage ?: "Không thể gửi lời mời kết bạn (${response.code()})"
                )
            }
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "Không thể gửi lời mời kết bạn (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Lỗi mạng: không thể gửi lời mời kết bạn",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Không thể gửi lời mời kết bạn",
                throwable = e
            )
        }
    }

    private fun extractServerMessage(raw: String): String? {
        if (raw.isBlank()) return null
        val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(raw)?.groupValues?.getOrNull(1)
    }

    override suspend fun getShareFriends(currentUserId: String): ApiResult<List<FriendRecipient>> {
        if (currentUserId.isBlank()) {
            return ApiResult.Error(message = "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh t\u00e0i kho\u1ea3n hi\u1ec7n t\u1ea1i")
        }

        return try {
            // TODO(BE): Provide a dedicated friends endpoint to return full friend graph.
            // Current FE extracts recipients from real conversation participants.
            val conversations = conversationApi.getMyConversations()
            val uniqueRecipients = LinkedHashMap<String, FriendRecipient>()

            conversations.forEach { conversation ->
                conversation.participants.orEmpty().forEach { participant ->
                    val participantId = participant.id.orEmpty()
                    if (participantId.isBlank() || participantId == currentUserId) return@forEach

                    if (!uniqueRecipients.containsKey(participantId)) {
                        val displayName = participant.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: participant.username?.takeIf { it.isNotBlank() }
                            ?: "Ng\u01b0\u1eddi d\u00f9ng"

                        uniqueRecipients[participantId] = FriendRecipient(
                            id = participantId,
                            displayName = displayName,
                            username = participant.username ?: "",
                            avatarUrl = participant.avatarUrl,
                            isOnline = false  // Default to false since we don't have this info from conversation
                        )
                    }
                }
            }

            ApiResult.Success(uniqueRecipients.values.toList())
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "L\u1ed7i m\u00e1y ch\u1ee7 (${e.code()}) khi t\u1ea3i danh s\u00e1ch b\u1ea1n b\u00e8",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "L\u1ed7i m\u1ea1ng: kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch b\u1ea1n b\u00e8",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch b\u1ea1n b\u00e8",
                throwable = e
            )
        }
    }
}
