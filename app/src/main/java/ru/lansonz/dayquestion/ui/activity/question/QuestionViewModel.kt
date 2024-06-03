package ru.lansonz.dayquestion.ui.activity.question

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import ru.lansonz.dayquestion.model.QuestionModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs

class QuestionViewModel() : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private var prefs: Prefs = Prefs.getInstance(MyApplication.getInstance())

    private val _selectedQuestion = MutableLiveData<QuestionModel?>()
    val selectedQuestion: MutableLiveData<QuestionModel?> get() = _selectedQuestion

    private val _cachedQuestions = MutableLiveData<List<QuestionModel>>()
    val cachedQuestions: LiveData<List<QuestionModel>> get() = _cachedQuestions

    data class QuestionStat(
        val questionId: String,
        val quesCount: Int,
        val ansCount: Int,
        val coef: Double
    )

    // Функция для создания вопроса и ответов к нему
// Функция для создания вопроса и ответов к нему
    fun createQuestionAndAnswers(userID: String?, question: String?, answers: List<String?>): String {
        val questionRef = database.reference.child("questions").push()
        val questionData = hashMapOf(
            "questionID" to questionRef.key,
            "question" to question,
            "authorID" to userID,
            "timestamp" to ServerValue.TIMESTAMP,
            "answers" to answers
        )

        questionRef.setValue(questionData)
            .addOnSuccessListener {
                Log.d("RealtimeDatabase", "Question and answers successfully added!")
                // Создание документа в questions_stat
                createQuestionStat(questionRef.key)
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDatabase", "Error adding question: $e")
            }

        return questionRef.key ?: ""
    }

    // Функция для создания записи в questions_stat
    private fun createQuestionStat(questionID: String?) {
        if (questionID == null) return

        val statRef = database.reference.child("questions_stat").child(questionID)
        val statData = hashMapOf(
            "questionID" to questionID,
            "ques_count" to 0,
            "ans_count" to 0,
            "coef" to 0.0
        )

        statRef.setValue(statData)
            .addOnSuccessListener {
                Log.d("RealtimeDatabase", "Stat document successfully created for question ID: $questionID")
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDatabase", "Error creating stat document for question ID $questionID: $e")
            }
    }

    // Пример использования функции создания вопроса
    fun exampleUsage() {
        val userID = "exampleUserID"
        val question = "What is your favorite color?"
        val answers = listOf("Red", "Blue", "Green", "Other")

        val questionID = createQuestionAndAnswers(userID, question, answers)
        Log.d("ExampleUsage", "Created question with ID: $questionID")
    }

    // Функция для сохранения вопроса в истории
    fun saveQuestionToHistory(questionID: String, userID: String, answered: Boolean) {
        val historyRef = database.reference.child("user_history").push()
        val questionHistoryData = hashMapOf(
            "questionID" to questionID,
            "userID" to userID,
            "answered" to answered,
            "timestamp" to ServerValue.TIMESTAMP
        )

        historyRef.setValue(questionHistoryData)
            .addOnSuccessListener {
                Log.d("RealtimeDatabase", "Question successfully saved to history!")
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDatabase", "Error saving question to history: $e")
            }
    }

    // Функция для увеличения счётчика вопросов
    fun incrementQuestionCount(questionID: String) {
        val statRef = database.reference.child("questions_stat").child(questionID)

        statRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    statRef.child("ques_count").setValue((snapshot.child("ques_count").value as Long) + 1)
                        .addOnSuccessListener {
                            Log.d("RealtimeDatabase", "Question count incremented successfully for question ID: $questionID")
                        }
                        .addOnFailureListener { e ->
                            Log.e("RealtimeDatabase", "Error incrementing question count for question ID $questionID: $e")
                        }
                } else {
                    val newData = hashMapOf(
                        "questionID" to questionID,
                        "ques_count" to 1,
                        "ans_count" to 0,
                        "coef" to 0.0
                    )
                    statRef.setValue(newData)
                        .addOnSuccessListener {
                            Log.d("RealtimeDatabase", "New document created for question ID: $questionID")
                        }
                        .addOnFailureListener { e ->
                            Log.e("RealtimeDatabase", "Error creating new document for question ID $questionID: $e")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDatabase", "Error checking existence of document for question ID $questionID: $error")
            }
        })
    }

    // Функция для получения вопроса для пользователя
    fun getUniqueOrOldestQuestion(userId: String) {
        Log.d("QuestionRepository", "Fetching questions for userId: $userId")

        // Step 1: Get the array of question IDs from user_history
        database.reference.child("user_history").orderByChild("userID").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userHistoryQuestionIds = snapshot.children.mapNotNull { it.child("questionID").getValue(String::class.java) }
                    Log.d("QuestionRepository", "User history question IDs: $userHistoryQuestionIds")

                    // Step 2: Find questions in questions_stat with high coef and ques_count that haven't been asked to the user and weren't created by the user
                    database.reference.child("questions_stat").orderByChild("coef").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(statSnapshot: DataSnapshot) {
                            val potentialQuestions = statSnapshot.children.mapNotNull {
                                val questionId = it.child("questionID").getValue(String::class.java) ?: return@mapNotNull null
                                val quesCount = it.child("ques_count").getValue(Int::class.java) ?: return@mapNotNull null
                                val ansCount = it.child("ans_count").getValue(Int::class.java) ?: return@mapNotNull null
                                val coef = it.child("coef").getValue(Double::class.java) ?: return@mapNotNull null

                                if (!userHistoryQuestionIds.contains(questionId)) {
                                    QuestionStat(questionId, quesCount, ansCount, coef)
                                } else {
                                    null
                                }
                            }.sortedWith(compareByDescending<QuestionStat> { it.coef }.thenByDescending { it.quesCount })

                            Log.d("QuestionRepository", "Potential questions: $potentialQuestions")

                            if (potentialQuestions.isNotEmpty()) {
                                val selectedQuestion = potentialQuestions.first()
                                Log.d("QuestionRepository", "Selected question from potential questions: $selectedQuestion")
                                fetchQuestion(selectedQuestion)
                            } else {
                                // Step 3: Get the oldest asked question from user_history
                                val oldestQuestion = snapshot.children.minByOrNull { it.child("timestamp").getValue(Long::class.java) ?: Long.MAX_VALUE }
                                val oldestQuestionId = oldestQuestion?.child("questionID")?.getValue(String::class.java)
                                Log.d("QuestionRepository", "Oldest question ID: $oldestQuestionId")

                                oldestQuestionId?.let {
                                    fetchQuestion(QuestionStat(it, oldestQuestion?.child("ques_count")?.getValue(Int::class.java) ?: 0, oldestQuestion?.child("ans_count")?.getValue(Int::class.java) ?: 0, oldestQuestion?.child("coef")?.getValue(Double::class.java) ?: 0.0))
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("QuestionRepository", "Error fetching questions_stat: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("QuestionRepository", "Error fetching user history: ${error.message}")
                }
            })
    }

    private fun fetchQuestion(questionStat: QuestionStat) {
        Log.d("QuestionRepository", "Fetching question details for questionId: ${questionStat.questionId}")

        database.reference.child("questions").child(questionStat.questionId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val question = snapshot.getValue(QuestionModel::class.java)?.copy(
                    id = snapshot.child("questionID").getValue(String::class.java),
                    text = snapshot.child("question").getValue(String::class.java),
                    answers = snapshot.child("answers").children.mapNotNull { it.getValue(String::class.java) },
                    ques_count = questionStat.quesCount,
                    ans_count = questionStat.ansCount,
                    coef = questionStat.coef,
                    authorID = snapshot.child("authorID").getValue(String::class.java),
                    timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                )
                Log.d("QuestionRepository", "Fetched question details: $question")
                _selectedQuestion.value = question
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("QuestionRepository", "Error fetching question details: ${error.message}")
            }
        })
    }

    // Функция для получения вопросов и сохранения их в SharedPreferences
    fun getQuestions(userID: String) {
        val userHistoryRef = database.reference.child("user_history").orderByChild("userID").equalTo(userID)

        userHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(historySnapshot: DataSnapshot) {
                // Логируем все вопросы, найденные в истории пользователя
                for (snapshot in historySnapshot.children) {
                    val questionID = snapshot.child("questionID").value as String
                    val isAnswered = snapshot.child("answered").value as Boolean
                    Log.d("RealtimeDatabase", "Question found in user history - Question ID: $questionID, Answered: $isAnswered")

                    // Получаем содержание вопроса из базы данных
                    val questionRef = database.reference.child("questions").child(questionID)
                    questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(questionSnapshot: DataSnapshot) {
                            val questionModel = questionSnapshot.getValue(QuestionModel::class.java)
                            if (questionModel != null) {
                                Log.d("RealtimeDatabase", "Question fetched - ID: ${questionModel.id}, Text: ${questionModel.text}")

                                // Обновляем модель вопроса
                                questionModel.isAnswered = isAnswered

                                // Добавляем вопрос в список вопросов и сохраняем в SharedPreferences
                                val updatedQuestions = _cachedQuestions.value?.toMutableList() ?: mutableListOf()
                                updatedQuestions.add(questionModel)
                                prefs.saveQuestions(updatedQuestions)
                                _cachedQuestions.postValue(updatedQuestions)
                            } else {
                                Log.e("RealtimeDatabase", "Failed to fetch question with ID: $questionID")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("RealtimeDatabase", "Error getting question: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDatabase", "Error getting user history: ${error.message}")
            }
        })
    }

    fun loadCachedQuestions() {
        val questions = prefs.getQuestions()
        if (questions.isNotEmpty()) {
            val sortedQuestions = questions.sortedByDescending { it.timestamp }
            _cachedQuestions.postValue(sortedQuestions)
        }
    }

    companion object {
        private var instance : QuestionViewModel? = null
        fun getInstance() =
            instance ?: synchronized(QuestionViewModel::class.java){
                instance ?: QuestionViewModel().also { instance = it }
            }
    }
}

class ViewModelFactory : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass){
            when {
                isAssignableFrom(QuestionViewModel::class.java) -> QuestionViewModel.getInstance()
                else -> throw IllegalArgumentException("Unknown viewModel class $modelClass")
            }
        } as T


    companion object {
        private var instance : ViewModelFactory? = null
        fun getInstance() =
            instance ?: synchronized(ViewModelFactory::class.java){
                instance ?: ViewModelFactory().also { instance = it }
            }
    }
}
