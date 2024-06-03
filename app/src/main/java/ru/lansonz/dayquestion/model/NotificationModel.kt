package ru.lansonz.dayquestion.model

data class NotificationModel(
    val userName: String,
    val time: String,
    val message: String,
    val replies: String
)

/*
data class NotificationModel (
    var notificationID: String = "",
    var status: String = "",
    val message: String,
    val replies: String,
    val time: String,
    var userID: String = "",
    var authorID: String = ""
)
*/

