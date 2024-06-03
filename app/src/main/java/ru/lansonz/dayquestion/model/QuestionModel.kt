package ru.lansonz.dayquestion.model

import com.google.firebase.Timestamp
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class QuestionModel(
    val id: String? = "",
    val text: String? = "",
    val answers: List<String>? = listOf(),
    val ques_count: Int? = 0,
    val ans_count: Int? = 0,
    val coef: Double? = 0.0,
    var isAnswered: Boolean? = false,
    val authorID: String? = "",
    val timestamp: Long? = null
)
