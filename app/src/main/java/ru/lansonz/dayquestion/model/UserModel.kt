package ru.lansonz.dayquestion.model

import com.google.gson.Gson

data class UserModel(
    var userID: String = "",
    var email: String = "",
    var fullName: String = "",
    var address: String = "",
    var profilePictureURL: String = "",
    var selected: Boolean = false,
    var active: Boolean = false,
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): UserModel = Gson().fromJson(json, UserModel::class.java)
    }
}

data class userStats(
    var userID: String = "",
    var points: Int = 0,
    var rating: Int = 0,
    var ques_count: Int = 0,
    var ans_count: Int = 0
)