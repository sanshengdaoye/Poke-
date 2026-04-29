package com.pocketbook.service

import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.Transaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartCategoryClassifier @Inject constructor() {

    /**
     * 基于用户历史交易数据，学习"商户名/备注→分类"的映射规则
     * 
     * @param note 交易备注
     * @param merchant 商户名（可为null）
     * @param userHistory 用户历史交易列表
     * @return 推断出的分类，如果历史数据不足则返回null
     */
    fun classify(note: String?, merchant: String?, userHistory: List<Transaction>): Category? {
        if (userHistory.isEmpty()) return null

        val searchKey = buildSearchKey(note, merchant)
        if (searchKey.isBlank()) return null

        // 统计历史交易中相同/相似note对应的分类频率
        val categoryFrequency = mutableMapOf<String, Int>()
        val noteToCategory = mutableMapOf<String, String>()

        userHistory.forEach { transaction ->
            val historyNote = transaction.note ?: ""
            val historyKey = buildSearchKey(historyNote, null)
            val categoryId = transaction.categoryId ?: return@forEach

            // 精确匹配
            if (historyKey == searchKey) {
                categoryFrequency[categoryId] = categoryFrequency.getOrDefault(categoryId, 0) + 3
            }
            // 包含匹配（note包含搜索词或搜索词包含note）
            else if (historyKey.contains(searchKey) || searchKey.contains(historyKey)) {
                categoryFrequency[categoryId] = categoryFrequency.getOrDefault(categoryId, 0) + 2
            }
            // 模糊匹配：关键词重叠
            else {
                val historyWords = historyKey.split(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]+")).filter { it.length >= 2 }
                val searchWords = searchKey.split(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]+")).filter { it.length >= 2 }
                val overlap = historyWords.intersect(searchWords.toSet()).size
                if (overlap > 0) {
                    categoryFrequency[categoryId] = categoryFrequency.getOrDefault(categoryId, 0) + overlap
                }
            }
        }

        // 取最高频的分类
        val bestCategoryId = categoryFrequency.maxByOrNull { it.value }?.key ?: return null
        val bestScore = categoryFrequency[bestCategoryId] ?: 0

        // 阈值：至少要有2分（即至少一次包含匹配或精确匹配）
        if (bestScore < 2) return null

        // 返回Category对象（仅包含id，调用方需要从CategoryRepository获取完整信息）
        return Category(
            id = bestCategoryId,
            name = "", // 调用方需要回填
            type = com.pocketbook.data.entity.CategoryType.EXPENSE // 默认支出，调用方需要确认
        )
    }

    /**
     * 批量学习：从历史数据中提取所有"关键词→分类"映射规则
     * 可用于预计算分类规则缓存
     */
    fun extractRules(userHistory: List<Transaction>): Map<String, String> {
        val rules = mutableMapOf<String, String>()
        val frequency = mutableMapOf<String, MutableMap<String, Int>>()

        userHistory.forEach { transaction ->
            val note = transaction.note ?: return@forEach
            val categoryId = transaction.categoryId ?: return@forEach
            val key = buildSearchKey(note, null)
            if (key.isBlank()) return@forEach

            val catMap = frequency.getOrPut(key) { mutableMapOf() }
            catMap[categoryId] = catMap.getOrDefault(categoryId, 0) + 1
        }

        frequency.forEach { (key, catMap) ->
            val bestCategory = catMap.maxByOrNull { it.value }?.key
            if (bestCategory != null && catMap[bestCategory]!! >= 2) {
                rules[key] = bestCategory
            }
        }

        return rules
    }

    private fun buildSearchKey(note: String?, merchant: String?): String {
        val parts = mutableListOf<String>()
        merchant?.let { if (it.isNotBlank()) parts.add(it.trim()) }
        note?.let { if (it.isNotBlank()) parts.add(it.trim()) }
        return parts.joinToString(" ").lowercase()
    }
}
