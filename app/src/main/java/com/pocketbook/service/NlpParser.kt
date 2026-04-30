package com.pocketbook.service

import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.CategoryType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NlpParser @Inject constructor() {

    /**
     * 解析自然语言输入，提取金额、分类、备注
     * 支持格式："午餐35"、"打车20块"、"工资5000"、"昨天买咖啡28"
     */
    fun parse(input: String, categories: List<Category>): NlpResult? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // 提取金额：找最后一个数字（支持小数）
        val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
        val amountMatch = amountRegex.findAll(trimmed).lastOrNull()
        val amountStr = amountMatch?.value ?: return null

        // 金额前的文本作为分类关键词
        val beforeAmount = trimmed.substring(0, amountMatch.range.first).trim()
        // 金额后的文本作为备注
        val afterAmount = trimmed.substring(amountMatch.range.last + 1).trim()

        // 匹配分类（基于关键词模糊匹配）
        val matchedCategory = matchCategory(beforeAmount, categories)

        // 备注 = 金额后文本，如果没有则取关键词
        val note = afterAmount.takeIf { it.isNotEmpty() } ?: beforeAmount

        // 判断收支类型
        val type = detectType(beforeAmount, amountStr, matchedCategory)

        return NlpResult(
            amount = amountStr,
            category = matchedCategory,
            note = note,
            type = type
        )
    }

    private fun matchCategory(keyword: String, categories: List<Category>): Category? {
        if (keyword.isEmpty()) return null

        // 按优先级匹配
        return categories.find { cat ->
            // 精确匹配
            cat.name == keyword || keyword.contains(cat.name) || cat.name.contains(keyword)
        } ?: categories.find { cat ->
            // 别名匹配
            getAliases(cat.name).any { alias ->
                keyword.contains(alias) || alias.contains(keyword)
            }
        } ?: categories.maxByOrNull { cat ->
            // 字符重叠度
            val catChars = cat.name.toSet()
            val keywordChars = keyword.toSet()
            catChars.intersect(keywordChars).size
        }?.takeIf {
            // 至少有一个字符重叠
            it.name.toSet().intersect(keyword.toSet()).isNotEmpty()
        }
    }

    private fun getAliases(categoryName: String): List<String> {
        return when (categoryName) {
            "餐饮" -> listOf("餐", "吃", "饭", "食", "午餐", "晚餐", "早餐", "外卖", "食堂", "奶茶", "咖啡", "火锅", "烧烤", "水果")
            "交通" -> listOf("车", "路", "地铁", "公交", "打车", "滴滴", "油费", "加油", "高速", "停车", "高铁", "飞机", " taxi")
            "购物" -> listOf("买", "购", "东西", "淘宝", "京东", "衣服", "鞋", "包", "化妆品", "超市", "便利店", "零食")
            "娱乐" -> listOf("玩", "电影", "游戏", "唱", "KTV", "酒吧", "按摩", "足疗", "旅游", "旅行", "门票")
            "住房" -> listOf("房", "租", "贷", "物业", "水电", "煤气", "燃气", "装修", "家具", "家电", "房租", "房贷")
            "医疗" -> listOf("药", "病", "医", "挂号", "体检", "疫苗", "医院", "诊所", "牙医", "眼镜")
            "教育" -> listOf("学", "课", "书", "培训", "考试", "学费", "资料", "教材", "网课", "补习", "考研")
            "通讯" -> listOf("话", "网", "流量", "宽带", "手机", "话费", "电信", "移动", "联通", "WiFi")
            "人情" -> listOf("礼", "红包", "请客", "份子", "送礼", "红包", "压岁钱", "拜年", "酒席")
            "工资" -> listOf("薪", "工资", "收入", "奖金", "兼职", "外快", "稿费", "分红", "补贴", "报销")
            "奖金" -> listOf("奖", "红包", "绩效", "年终奖", "季度奖", "全勤奖")
            "投资" -> listOf("股", "基金", "理财", "利息", "分红", "比特币", "债券", "黄金")
            "兼职" -> listOf("兼", "外快", "私活", " freelance", "临时工")
            "红包" -> listOf("红", "压岁钱", "拜年", "恭喜")
            "其他支出" -> listOf("其他", "杂")
            "其他收入" -> listOf("其他", "杂", "意外")
            else -> emptyList()
        }
    }

    private fun detectType(keyword: String, amount: String, category: Category?): CategoryType {
        // 根据关键词判断收支
        val incomeKeywords = listOf("工资", "薪", "奖金", "收入", "兼职", "外快", "投资", "分红", "报销", "红包", "理财", "利息", "退款", "赔", "补")
        if (incomeKeywords.any { keyword.contains(it) }) return CategoryType.INCOME

        // 根据分类判断
        if (category != null) return category.type

        // 默认支出
        return CategoryType.EXPENSE
    }
}

data class NlpResult(
    val amount: String,
    val category: Category?,
    val note: String,
    val type: CategoryType
)
