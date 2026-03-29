package com.example.frontend.presentation.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.FriendUseCase.AcceptFriendRequestUseCase
import com.example.frontend.domain.usecase.FriendUseCase.AddFriendUseCase
import com.example.frontend.domain.usecase.FriendUseCase.CancelFriendRequestUseCase
import com.example.frontend.domain.usecase.FriendUseCase.DeleteFriendUseCase
import com.example.frontend.domain.usecase.FriendUseCase.RejectFriendRequestUseCase
import com.example.frontend.domain.usecase.SearchUseCase.AddSearchHistoryUseCase
import com.example.frontend.domain.usecase.SearchUseCase.ClearSearchHistoryUseCase
import com.example.frontend.domain.usecase.SearchUseCase.DeleteSearchHistoryUseCase
import com.example.frontend.domain.usecase.SearchUseCase.GetSearchHistoryUseCase
import com.example.frontend.domain.usecase.SearchUseCase.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase,
    private val addFriendUseCase: AddFriendUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val cancelFriendRequestUseCase: CancelFriendRequestUseCase,
    private val deleteFriendUseCase: DeleteFriendUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val deleteSearchHistoryUseCase: DeleteSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "SearchViewModel"
        private val FRIEND_STATUSES = setOf("FRIEND", "FRIENDS", "ACCEPTED")
        private val SENT_PENDING_STATUSES = setOf("PENDING", "REQUEST_SENT", "OUTGOING_PENDING")
        private val INCOMING_PENDING_STATUSES = setOf("REQUEST_RECEIVED", "INCOMING_PENDING")
    }

    private fun normalizeFriendshipStatus(raw: String?): String {
        return raw.orEmpty().trim().uppercase()
    }

    init {
        loadHistory()
    }

    // ──────────────────────────── Lịch sử ────────────────────────────

    private fun loadHistory() {
        viewModelScope.launch {
            val history = getSearchHistoryUseCase()
            _uiState.update { it.copy(searchHistory = history) }
        }
    }

    fun deleteHistory(keyword: String) {
        viewModelScope.launch {
            deleteSearchHistoryUseCase(keyword)
            loadHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
            loadHistory()
        }
    }

    // ──────────────────────────── Query ────────────────────────────

    /**
     * Gọi mỗi khi text trong ô search thay đổi.
     * Reset kết quả cũ để tránh hiển thị stale data.
     */
    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                hasSearched = false,
                results = null,
                error = null
            )
        }
    }

    fun onScopeChange(scope: SearchScope) {
        _uiState.update { it.copy(scope = scope) }
    }

    /**
     * Chọn một mục trong lịch sử: điền vào ô search rồi tìm ngay.
     */
    fun selectHistory(keyword: String) {
        _uiState.update {
            it.copy(
                query = keyword,
                hasSearched = false,
                results = null,
                error = null
            )
        }
        search()
    }

    fun clearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                results = null,
                hasSearched = false,
                error = null
            )
        }
    }

    fun addFriend(friendId: String) {
        if (friendId.isBlank()) return

        val current = _uiState.value
        if (current.addingFriendIds.contains(friendId)) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(addingFriendIds = state.addingFriendIds + friendId)
            }

            when (val result = addFriendUseCase(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updatedResults = state.results?.let { res ->
                            res.copy(
                                users = res.users.map { user ->
                                    if (user.id == friendId) {
                                        user.copy(isFriend = false, friendshipStatus = "REQUEST_SENT")
                                    } else {
                                        user
                                    }
                                }
                            )
                        }

                        state.copy(
                            results = updatedResults,
                            addingFriendIds = state.addingFriendIds - friendId,
                            pendingSentFriendIds = state.pendingSentFriendIds + friendId,
                            pendingIncomingFriendIds = state.pendingIncomingFriendIds - friendId
                        )
                    }
                }

                is ApiResult.Error -> {
                    Log.w(TAG, "addFriend failed: ${result.message}")
                    _uiState.update { state ->
                        val msg = result.message.lowercase()
                        val hasIncoming = msg.contains("incoming friend request already exists")
                        val hasSent = msg.contains("friend request already exists") && !hasIncoming
                        val updatedResults = state.results?.let { res ->
                            res.copy(
                                users = res.users.map { user ->
                                    if (user.id != friendId) return@map user

                                    when {
                                        hasIncoming -> user.copy(isFriend = false, friendshipStatus = "REQUEST_RECEIVED")
                                        hasSent -> user.copy(isFriend = false, friendshipStatus = "REQUEST_SENT")
                                        else -> user
                                    }
                                }
                            )
                        }

                        state.copy(
                            results = updatedResults,
                            addingFriendIds = state.addingFriendIds - friendId,
                            pendingIncomingFriendIds = if (hasIncoming) {
                                state.pendingIncomingFriendIds + friendId
                            } else {
                                state.pendingIncomingFriendIds
                            },
                            pendingSentFriendIds = if (hasSent) {
                                state.pendingSentFriendIds + friendId
                            } else {
                                state.pendingSentFriendIds
                            }
                        )
                    }
                }
            }
        }
    }

    fun rejectRequest(userId: String) {
        viewModelScope.launch {
            when (rejectFriendRequestUseCase(userId)) {
                is ApiResult.Success -> {
                    updateFriendshipStatusLocal(userId, "NONE")
                    // Xóa user khỏi danh sách pending
                    _uiState.update {
                        it.copy(pendingIncomingFriendIds = it.pendingIncomingFriendIds - userId)
                    }
                }
                is ApiResult.Error -> { /* Xử lý hiển thị lỗi nếu cần */ }
            }
        }
    }

    fun acceptRequest(userId: String) {
        viewModelScope.launch {
            when (acceptFriendRequestUseCase(userId)) {
                is ApiResult.Success -> {
                    // Chấp nhận thành công, cập nhật isFriend = true và status = FRIEND
                    _uiState.update { state ->
                        val updatedResults = state.results?.copy(
                            users = state.results.users.map { user ->
                                if (user.id == userId) {
                                    user.copy(isFriend = true, friendshipStatus = "FRIEND")
                                } else user
                            }
                        )
                        state.copy(
                            results = updatedResults,
                            pendingIncomingFriendIds = state.pendingIncomingFriendIds - userId
                        )
                    }
                }
                is ApiResult.Error -> { /* Xử lý hiển thị lỗi nếu cần */ }
            }
        }
    }

    fun cancelRequest(userId: String) {
        viewModelScope.launch {
            when (cancelFriendRequestUseCase(userId)) {
                is ApiResult.Success -> {
                    updateFriendshipStatusLocal(userId, "NONE")
                    // Xóa user khỏi danh sách đã gửi
                    _uiState.update {
                        it.copy(pendingSentFriendIds = it.pendingSentFriendIds - userId)
                    }
                }
                is ApiResult.Error -> { /* Xử lý hiển thị lỗi nếu cần */ }
            }
        }
    }

    fun deleteFriend(friendId: String) {
        if (friendId.isBlank()) return

        val current = _uiState.value
        if (current.deletingFriendIds.contains(friendId)) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(deletingFriendIds = state.deletingFriendIds + friendId)
            }

            when (val result = deleteFriendUseCase(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updatedResults = state.results?.let { res ->
                            res.copy(
                                users = res.users.map { user ->
                                    if (user.id == friendId) {
                                        user.copy(isFriend = false, friendshipStatus = "NONE")
                                    } else {
                                        user
                                    }
                                }
                            )
                        }

                        state.copy(
                            results = updatedResults,
                            deletingFriendIds = state.deletingFriendIds - friendId,
                            pendingSentFriendIds = state.pendingSentFriendIds - friendId,
                            pendingIncomingFriendIds = state.pendingIncomingFriendIds - friendId
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(deletingFriendIds = state.deletingFriendIds - friendId, error = result.message)
                    }
                }
            }
        }
    }

    // ──────────────────────────── Search ────────────────────────────

    /**
     * Gửi keyword tới backend, lưu lịch sử, cập nhật UI.
     */
    fun search() {
        val keyword = _uiState.value.query.trim()
        if (keyword.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasSearched = true) }

            // Lưu lịch sử trước khi gọi API
            addSearchHistoryUseCase(keyword)
            loadHistory()

            when (val result = searchUseCase(keyword, _uiState.value.scope.value)) {
                is ApiResult.Success -> _uiState.update {
                    val normalizedUsers = result.data.users.map { user ->
                        val status = normalizeFriendshipStatus(user.friendshipStatus)
                        if (status in FRIEND_STATUSES && !user.isFriend) {
                            user.copy(isFriend = true)
                        } else {
                            user
                        }
                    }
                    val normalizedResult = result.data.copy(users = normalizedUsers)
                    val friendIds = normalizedUsers
                        .filter { user ->
                            user.isFriend || normalizeFriendshipStatus(user.friendshipStatus) in FRIEND_STATUSES
                        }
                        .map { user -> user.id }
                        .toSet()
                    val pendingSentIds = normalizedUsers
                        .filter { user ->
                            normalizeFriendshipStatus(user.friendshipStatus) in SENT_PENDING_STATUSES
                        }
                        .map { user -> user.id }
                        .toSet()
                    val pendingIncomingIds = normalizedUsers
                        .filter { user ->
                            normalizeFriendshipStatus(user.friendshipStatus) in INCOMING_PENDING_STATUSES
                        }
                        .map { user -> user.id }
                        .toSet()

                    it.copy(
                        isLoading = false,
                        results = normalizedResult,
                        pendingSentFriendIds = pendingSentIds - friendIds,
                        pendingIncomingFriendIds = pendingIncomingIds - friendIds
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun updateFriendshipStatusLocal(userId: String, newStatus: String) {
        _uiState.update { state ->
            val updatedResults = state.results?.copy(
                users = state.results.users.map { user ->
                    if (user.id == userId) user.copy(friendshipStatus = newStatus) else user
                }
            )
            state.copy(results = updatedResults)
        }
    }
}
