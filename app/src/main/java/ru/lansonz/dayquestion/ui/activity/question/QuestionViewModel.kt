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
import ru.lansonz.dayquestion.model.AnswerModel
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
    fun createQuestionAndAnswers(userID: String?, question: String?, answers: List<String?>): String {
        val questionRef = database.reference.child("questions").push()
        val answerModels = answers.map { AnswerModel(text = it ?: "") }
        val questionData = hashMapOf(
            "questionID" to questionRef.key,
            "question" to question,
            "authorID" to userID,
            "timestamp" to ServerValue.TIMESTAMP,
            "answers" to answerModels
        )

        questionRef.setValue(questionData)
            .addOnSuccessListener {
                Log.d("RealtimeDatabase", "Question and answers successfully added!")
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
        Log.d("QuestionRepository", "Fetching question details for questionId: ${questionStat.questionId}")

        database.reference.child("questions").child(questionStat.questionId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("QuestionRepository", "DataSnapshot exists: ${snapshot.value}")

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
                            Log.d("QuestionRepository", "Fetched question details: $question")
                            _selectedQuestion.value = question
                        } else {
                            Log.e("QuestionRepository", "One or more fields are null")
                        }
                    } else {
                        Log.e("QuestionRepository", "DataSnapshot does not exist")
                    }
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
                for (snapshot in historySnapshot.children) {
                    val questionID = snapshot.child("questionID").value as String
                    val isAnswered = snapshot.child("answered").value as Boolean
                    Log.d("RealtimeDatabase", "Question found in user history - Question ID: $questionID, Answered: $isAnswered")

                    val questionRef = database.reference.child("questions").child(questionID)
                    questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(questionSnapshot: DataSnapshot) {
                            val answers = questionSnapshot.child("answers").children.mapNotNull {
                                it.getValue(AnswerModel::class.java)
                            }
                            val questionModel = questionSnapshot.getValue(QuestionModel::class.java)?.copy(
                                answers = answers
                            )
                            if (questionModel != null) {
                                Log.d("RealtimeDatabase", "Question fetched - ID: ${questionModel.id}, Text: ${questionModel.text}")

                                questionModel.isAnswered = isAnswered

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
