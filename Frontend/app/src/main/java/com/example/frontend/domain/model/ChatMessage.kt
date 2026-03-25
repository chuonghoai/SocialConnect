import com.example.frontend.domain.model.Conversation

// 1. Model bọc ngoài cùng (Response trả về)
data class GetMessagesResponse(
    val messages: List<MessageItem>,
    val meta: PaginationMeta
)

// 2. Model chi tiết của 1 tin nhắn
data class MessageItem(
    val id: String,
    val type: String,               // Ví dụ: "TEXT", "MEDIA", "SYSTEM"...
    val text: String,
    val isRecall: Boolean,
    val createAt: String,           // Chuỗi ISO 8601, bạn có thể map sang Date/LocalDateTime nếu cần
    val replyToMessageId: String?,  // Có thể null nếu không phải tin nhắn reply
    val sender: MessageSender,
    val media: List<MessageMedia>,
    val isRead: Boolean = false
)

// 3. Model thông tin người gửi
data class MessageSender(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

// 4. Model cho file đính kèm (Ảnh, Video, Âm thanh)
data class MessageMedia(
    val publicId: String,
    val secureUrl: String,
    val type: String                // Ví dụ: "IMAGE", "VIDEO"
)

// 5. Model phân trang
data class PaginationMeta(
    val totalItems: Int,
    val itemCount: Int,
    val itemsPerPage: Int,
    val totalPages: Int,
    val currentPage: Int
)

// 6. Model nhận event từ socket
data class NewMessageEvent(
    val conversationId: String,
    val message: MessageItem,
    val conversation: Conversation
)