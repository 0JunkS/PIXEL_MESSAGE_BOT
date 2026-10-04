package com.pixelbot.messenger.model

enum class MatchType(val displayName: String) {
    EXACT("완전 일치"),
    STARTS_WITH("시작 단어"),
    CONTAINS("포함 단어")
}

data class MacroRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var trigger: String,
    var matchType: MatchType = MatchType.EXACT,
    var response: String,
    var isEnabled: Boolean = true
)
