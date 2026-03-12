package com.example.frontend.presentation.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
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
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val deleteSearchHistoryUseCase: DeleteSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

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
                    it.copy(isLoading = false, results = result.data)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
