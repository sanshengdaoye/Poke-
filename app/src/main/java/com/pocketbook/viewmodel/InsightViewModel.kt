package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Insight
import com.pocketbook.repository.InsightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val insightRepository: InsightRepository
) : ViewModel() {

    private val _insights = MutableStateFlow<List<Insight>>(emptyList())
    val insights: StateFlow<List<Insight>> = _insights.asStateFlow()

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            try {
                // 简化实现：直接返回空列表
                // 实际应用中应从默认账本获取洞察
                _insights.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsRead(insightId: String) {
        viewModelScope.launch {
            try {
                insightRepository.markAsRead(insightId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
