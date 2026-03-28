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

    fun search() {
        val keyword = _uiState.value.query.trim()
        if (keyword.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasSearched = true) }

            runCatching {
                addSearchHistoryUseCase(keyword)
                loadHistory()
                searchUseCase(keyword, _uiState.value.scope.value)
            }.onSuccess { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(isLoading = false, results = result.data, error = null)
                    }

                    is ApiResult.Error -> _uiState.update {
                        it.copy(isLoading = false, error = normalizeSearchError(result.message))
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = normalizeSearchError(error.message)
                    )
                }
            }
        }
    }

    private fun normalizeSearchError(raw: String?): String {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isBlank()) {
            return "Không thể tìm kiếm lúc này. Vui lòng thử lại."
        }

        val lower = normalized.lowercase()
        if (lower.contains("unable to create converter")) {
            return "Không thể đọc dữ liệu tìm kiếm. Vui lòng thử lại."
        }
        if (lower.contains("không xác định") || lower.contains("unexpected")) {
            return "Không thể xử lý kết quả tìm kiếm. Vui lòng thử lại."
        }

        return normalized
    }
}
