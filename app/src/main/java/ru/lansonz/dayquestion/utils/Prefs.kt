package ru.lansonz.dayquestion.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.lansonz.dayquestion.model.NotificationModel
import ru.lansonz.dayquestion.model.QuestionModel
import ru.lansonz.dayquestion.model.UserModel

class Prefs private constructor(context: Context) {
    private val mPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    var hasCompletedWalkthrough: Boolean
        get() = mPrefs.getBoolean(ISFIRSTRUN, true)
        set(isFirstRun) {
            mPrefs.edit().putBoolean(ISFIRSTRUN, isFirstRun).apply()
        }

    var isNewUser: Boolean
        get() = mPrefs.getBoolean(ISNEWUSER, true)
        set(isNewUser) {
            mPrefs.edit().putBoolean(ISNEWUSER, isNewUser).apply()
        }

    var isQuestionAskedToday: Boolean
        get() = mPrefs.getBoolean(QUESTION_ASKED_TODAY, false)
        set(value) {
            mPrefs.edit().putBoolean(QUESTION_ASKED_TODAY, value).apply()
        }

    // Сохранение уведомлений
    fun saveNotifications(notifications: List<NotificationModel>) {
        val json = gson.toJson(notifications)
        mPrefs.edit().putString(NOTIFICATIONS, json).apply()
    }

    // Получение уведомлений
    fun getNotifications(): List<NotificationModel> {
        val json = mPrefs.getString(NOTIFICATIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<NotificationModel>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveSelectedQuestion(question: QuestionModel?) {
        val json = gson.toJson(question)
        mPrefs.edit().putString(SELECTED_QUESTION, json).apply()
    }

    fun getSelectedQuestion(): QuestionModel? {
        val json = mPrefs.getString(SELECTED_QUESTION, null)
        return if (json != null) {
            gson.fromJson(json, QuestionModel::class.java)
        } else {
            null
        }
    }

    // Сохранение вопросов
    fun saveQuestions(questions: List<QuestionModel>) {
        val json = gson.toJson(questions)
        mPrefs.edit().putString(QUESTIONS, json).apply()
    }

    // Получение вопросов
    fun getQuestions(): List<QuestionModel> {
        val json = mPrefs.getString(QUESTIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<QuestionModel>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // В классе Prefs добавьте методы для сохранения и получения UserModel
    fun saveUser(user: UserModel) {
        val json = user.toJson()
        mPrefs.edit().putString(USER, json).apply()
    }

    fun getUser(): UserModel? {
        val json = mPrefs.getString(USER, null)
        return json?.let { UserModel.fromJson(it) }
    }

    // Метод для очистки вопросов
    fun clearQuestions() {
        mPrefs.edit().remove(QUESTIONS).apply()
    }

    // Метод для очистки всех данных
    fun clearAll() {
        mPrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "my_prefs"
        private const val USER = "user"
        private const val ISNEWUSER = "isNewUser"
        private const val ISFIRSTRUN = "hasCompletedWalkthrough"
        private const val NOTIFICATIONS = "notifications"
        private const val QUESTIONS = "questions"
        private const val SELECTED_QUESTION = "selected_question"
        private const val QUESTION_ASKED_TODAY = "question_asked_today"

        @Volatile
        private var instance: Prefs? = null

        fun getInstance(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }
}
