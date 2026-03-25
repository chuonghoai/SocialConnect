package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.ConversationApi
import com.example.frontend.domain.model.FriendRecipient
import com.example.frontend.domain.repository.FriendRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi
) : FriendRepository {

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
                            avatarUrl = participant.avatarUrl
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
