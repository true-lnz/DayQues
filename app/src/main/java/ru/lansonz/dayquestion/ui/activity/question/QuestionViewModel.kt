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
import com.google.firebase.database.getValue
import ru.lansonz.dayquestion.model.AnswerModel
import ru.lansonz.dayquestion.model.QuestionModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs

class QuestionViewModel() : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private var prefs: Prefs = Prefs.getInstance(MyApplication.getInstance())

    private val _selectedQuestion = MutableLiveData<QuestionModel?>()
    val selectedQuestion: MutableLiveData<QuestionModel?> get() = _selectedQuestion

    private val _currentQuestion = MutableLiveData<QuestionModel?>()
    val currentQuestion: MutableLiveData<QuestionModel?> get() = _currentQuestion

    private val _cachedQuestions = MutableLiveData<List<QuestionModel>>()
    val cachedQuestions: LiveData<List<QuestionModel>> get() = _cachedQuestions

    private val _cachedQuestions2 = MutableLiveData<List<QuestionModel>>()
    val cachedQuestions2: LiveData<List<QuestionModel>> get() = _cachedQuestions2

    data class QuestionStat(
        val questionId: String,
        val quesCount: Int,
        val ansCount: Int,
        val coef: Double
    )

    fun setCurrentQuestion(q: QuestionModel) {
        _currentQuestion.value = q
    }
    // Функция для создания вопроса и ответов к нему
    fun createQuestionAndAnswers(userID: String?, question: String?, answers: List<String?>): QuestionModel {
        // Получаем ссылку на новую запись вопроса
        val questionRef = database.reference.child("questions").push()

        // Создаем список моделей ответов
        val answerModels = answers.map { AnswerModel(text = it ?: "") }

        // Создаем объект QuestionModel
        val questionModel = QuestionModel(
            id = questionRef.key ?: "",
            text = question ?: "",
            answers = answerModels,
            authorID = userID ?: "",
            timestamp = System.currentTimeMillis() // Локальное время, т.к. ServerValue.TIMESTAMP не совместим напрямую
        )

        // Преобразование данных для Firebase
        val questionMap = hashMapOf(
            "id" to questionModel.id,
            "text" to questionModel.text,
            "authorID" to questionModel.authorID,
            "timestamp" to ServerValue.TIMESTAMP,  // Используем ServerValue.TIMESTAMP для серверного времени
            "answers" to answerModels.map { mapOf("text" to it.text, "ans_count" to it.ans_count) },
            "ques_count" to questionModel.ques_count,
            "ans_count" to questionModel.ans_count,
            "coef" to questionModel.coef,
            "isAnswered" to questionModel.isAnswered
        )

        setCurrentQuestion(questionModel)

        // Сохраняем данные в Firebase
        questionRef.setValue(questionMap)
            .addOnSuccessListener {
                Log.d("RealtimeDatabase", "Question and answers successfully added!")
                createQuestionStat(questionRef.key)
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDatabase", "Error adding question: $e")
            }

        // Возвращаем объект QuestionModel
        return questionModel
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

    // Функция для сохранения вопроса в истории
    fun saveQuestionToHistory(q: QuestionModel, userID: String, ans_index: Int = 0) {
        var answered = ans_index!=0
        val historyRef = database.reference.child("user_history").push()
        val questionHistoryData = hashMapOf(
            "questionID" to q.id,
            "userID" to userID,
            "answered" to answered,
            "timestamp" to ServerValue.TIMESTAMP
        )

        historyRef.setValue(questionHistoryData)
            .addOnSuccessListener {
                Log.d("RealtimeDatabase", "Question successfully saved to history!")
                incrementQuestionAndAnswerCount(q, ans_index)
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDatabase", "Error saving question to history: $e")
            }
    }

    // Функция для увеличения счётчика вопросов
    fun incrementQuestionAndAnswerCount(question: QuestionModel, ansIndex: Int) {
        val questionRef = database.reference.child("questions").child(question.id)

        questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Получаем текущее значение ans_count для конкретного ответа
                    val currentAnswers = snapshot.child("answers").children.map {
                        it.getValue(AnswerModel::class.java) ?: AnswerModel()
                    }

                    val updatedAnswers = currentAnswers.mapIndexed { index, answer ->
                        if (index == ansIndex) {
                            answer.copy(ans_count = answer.ans_count + 1)
                        } else {
                            answer
                        }
                    }

                    // Обновляем вопрос с увеличенным ans_count
                    questionRef.child("answers").setValue(updatedAnswers)
                        .addOnSuccessListener {
                            Log.d("RealtimeDatabase", "Answer count incremented successfully for answer index: $ansIndex")
                        }
                        .addOnFailureListener { e ->
                            Log.e("RealtimeDatabase", "Error incrementing answer count for answer index $ansIndex: $e")
                        }
                } else {
                    Log.e("RealtimeDatabase", "Question not found for ID: ${question.id}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDatabase", "Error accessing question for ID ${question.id}: $error")
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
        database.reference.child("questions").child(questionStat.questionId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val answers = snapshot.child("answers").children.mapNotNull {
                            it.getValue(AnswerModel::class.java)
                        }
                        val questionID = snapshot.child("questionID").getValue(String::class.java)
                        val text = snapshot.child("question").getValue(String::class.java)
                        val authorID = snapshot.child("authorID").getValue(String::class.java)
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java)

                        if (questionID != null && text != null && authorID != null && timestamp != null) {
                            val question = QuestionModel(
                                id = questionID,
                                text = text,
                                answers = answers,
                                ques_count = questionStat.quesCount,
                                ans_count = questionStat.ansCount,
                                coef = questionStat.coef,
                                authorID = authorID,
                                timestamp = timestamp
                            )
                            _selectedQuestion.value = question
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // Функция для получения вопросов и сохранения их в SharedPreferences
    fun getLastQuestions(userID: String) {
        val userHistoryRef = database.reference
            .child("user_history")
            .orderByChild("timestamp")
            .limitToLast(7)

        userHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(historySnapshot: DataSnapshot) {
                if (historySnapshot.exists()) {
                    val questionsList = mutableListOf<QuestionModel>()

                    for (snapshot in historySnapshot.children) {
                        val questionID = snapshot.child("questionID").getValue(String::class.java)
                        val isAnswered = snapshot.child("answered").getValue(Boolean::class.java) ?: true
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

                        questionID?.let { id ->
                            val questionRef = database.reference.child("questions").child(id)
                            questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(questionSnapshot: DataSnapshot) {
                                    if (questionSnapshot.exists()) {
                                        val text = questionSnapshot.child("question").getValue(String::class.java) ?: ""
                                        val answers = questionSnapshot.child("answers").children.mapNotNull {
                                            it.getValue(AnswerModel::class.java)
                                        }
                                        val authorID = questionSnapshot.child("authorID").getValue(String::class.java) ?: ""

                                        val questionModel = QuestionModel(
                                            id = id,
                                            text = text,
                                            answers = answers,
                                            authorID = authorID,
                                            timestamp = timestamp,
                                            isAnswered = isAnswered
                                        )

                                        val questionStatRef = database.reference.child("questions_stat").child(id)
                                        questionStatRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(statSnapshot: DataSnapshot) {
                                                if (statSnapshot.exists()) {
                                                    val quesCount = statSnapshot.child("ques_count").getValue(Int::class.java) ?: 0
                                                    val ansCount = statSnapshot.child("ans_count").getValue(Int::class.java) ?: 0
                                                    val coef = statSnapshot.child("coef").getValue(Double::class.java) ?: 0.0

                                                    val updatedQuestion = questionModel.copy(
                                                        ques_count = quesCount,
                                                        ans_count = ansCount,
                                                        coef = coef
                                                    )

                                                    questionsList.add(updatedQuestion)

                                                    // Ensure we only keep the latest 7 questions
                                                    if (questionsList.size > 7) {
                                                        questionsList.sortByDescending { it.timestamp }
                                                        questionsList.subList(7, questionsList.size).clear()
                                                    }

                                                    prefs.saveQuestions(questionsList)
                                                    _cachedQuestions.postValue(questionsList)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private val _questions = MutableLiveData<List<QuestionModel>>()
    val questions: LiveData<List<QuestionModel>> get() = _questions

    // Новая функция для получения всех вопросов пользователя
    fun getUserQuestions(userID: String) {
        val questionRef = database.reference.child("questions").child(userID)
        questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val questionsList = mutableListOf<QuestionModel>()
                    for (questionSnapshot in dataSnapshot.children) {
                        val text = questionSnapshot.child("question").getValue(String::class.java) ?: ""
                        val answers = questionSnapshot.child("answers").children.mapNotNull {
                            it.getValue(AnswerModel::class.java)
                        }
                        val authorID = questionSnapshot.child("authorID").getValue(String::class.java) ?: ""
                        val timestamp = questionSnapshot.child("timestamp").getValue(Long::class.java) ?: 0

                        val questionModel = QuestionModel(
                            id = questionSnapshot.key ?: "",
                            text = text,
                            answers = answers,
                            authorID = authorID,
                            timestamp = timestamp,
                            isAnswered = true
                        )
                        questionsList.add(questionModel)
                    }
                    _questions.value = questionsList
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun loadCachedQuestions() : Boolean {
        val questions = prefs.getQuestions()
        if (questions.isNotEmpty()) {
            val sortedQuestions = questions.sortedByDescending { it.timestamp }
            _cachedQuestions.postValue(sortedQuestions)
            return true
        }
        return false
    }

    fun clearCachedQuestions() {
        _cachedQuestions.value = emptyList()
        prefs.clearQuestions()
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
