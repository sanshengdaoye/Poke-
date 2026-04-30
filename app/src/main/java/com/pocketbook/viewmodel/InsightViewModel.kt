package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.repository.InsightRepository
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
                val allInsights = insightRepository.getAllInsights()
                _insights.value = allInsights.filter { !it.isDismissed }.sortedByDescending { it.generatedAt }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsRead(insightId: String) {
        viewModelScope.launch {
            insightRepository.markAsRead(insightId)
            loadInsights()
        }
    }

    fun dismissInsight(insightId: String) {
        viewModelScope.launch {
            insightRepository.dismissInsight(insightId)
            loadInsights()
        }
    }
}
