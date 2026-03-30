import com.example.frontend.domain.model.Conversation

data class GetMessagesResponse(
    val messages: List<MessageItem>,
    val meta: PaginationMeta
)

data class MessageItem(
    val id: String,
    val type: String,
    val text: String,
    val isRecall: Boolean,
    val createAt: String,
    val replyToMessage: RepliedMessageInfo?,
    val sender: MessageSender,
    val media: List<MessageMedia>,
    val isRead: Boolean = false
)

data class MessageSender(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

data class MessageMedia(
    val publicId: String,
    val secureUrl: String,
    val type: String
)

data class RepliedMessageInfo(
    val id: String,
    val type: String,
    val text: String,
    val isRecall: Boolean,
    val sender: MessageSender
)

data class PaginationMeta(
    val totalItems: Int,
    val itemCount: Int,
    val itemsPerPage: Int,
    val totalPages: Int,
    val currentPage: Int
)

data class NewMessageEvent(
    val conversationId: String,
    val message: MessageItem,
    val conversation: Conversation
)

data class MessageContextResponse(
    val messages: List<MessageItem>,
    val targetIndex: Int,
    val hasMorePrevious: Boolean,
    val hasMoreNext: Boolean
)