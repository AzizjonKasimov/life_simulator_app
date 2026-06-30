package com.azizjonkasimov.lifesimulator.domain.model

data class HistoryEntry(
    val day: Int,
    val title: String,
    val detail: String,
    val kind: HistoryKind,
)

enum class HistoryKind {
    ACTION,
    CAREER,
    DAY,
    EVENT,
    FINANCE,
    GOAL,
    RELATIONSHIP,
    SYSTEM,
}
