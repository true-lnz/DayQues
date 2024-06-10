package ru.lansonz.dayquestion.ui.fragment.question

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.model.NotificationModel
import ru.lansonz.dayquestion.model.QuestionModel
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs

class AddQuesFragment : Fragment() {
    private lateinit var questionEditText: EditText
    private lateinit var addAnswerButton: Button
    private lateinit var saveQuestionButton: Button
    private lateinit var answersBlock: LinearLayout
    private lateinit var answersContainer: LinearLayout
    private lateinit var questionTypeToggleGroup: MaterialButtonToggleGroup
    private lateinit var viewModel: QuestionViewModel
    private lateinit var q: QuestionModel
    private val answerViews = mutableListOf<View>()
    private var answerCount = 0
    private var lastCheckedId = View.NO_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(QuestionViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_ques, container, false)

        questionEditText = view.findViewById(R.id.questionEditText)
        addAnswerButton = view.findViewById(R.id.addAnswerButton)
        saveQuestionButton = view.findViewById(R.id.saveQuestionButton)
        answersContainer = view.findViewById(R.id.answersContainer)
        answersBlock = view.findViewById(R.id.answersBlock)
        questionTypeToggleGroup = view.findViewById(R.id.questionTypeToggleGroup)

        val btnChoice1: MaterialButton = view.findViewById(R.id.btn_choice_1)
        val btnChoice2: MaterialButton = view.findViewById(R.id.btn_choice_2)

        addAnswerButton.setOnClickListener { addAnswerField() }
        btnChoice1.setOnClickListener { handleToggleSelection(R.id.btn_choice_1) }
        btnChoice2.setOnClickListener { handleToggleSelection(R.id.btn_choice_2) }

        savedInstanceState?.let {
            lastCheckedId = it.getInt("lastCheckedId", R.id.btn_choice_1)
            questionTypeToggleGroup.check(lastCheckedId)
            if (lastCheckedId == R.id.btn_choice_2) {
                answersBlock.visibility = View.VISIBLE
            } else {
                answersBlock.visibility = View.GONE
            }
        } ?: run {
            questionTypeToggleGroup.check(R.id.btn_choice_1)
            answersBlock.visibility = View.GONE
            addAnswerField()
        }

        saveQuestionButton.setOnClickListener {
            saveQuestion()

            val notes =  listOf(
                NotificationModel("Вопрос Дня", "Cейчас", "Ваш Вопрос Дня Был опубликован", "Перейти"),
            )

            Prefs.getInstance(MyApplication.getInstance()).saveNotifications(notes)
            NotificationUtils.sendNotification(requireContext(), notes.get(0).userName, notes.get(0).message)


            findNavController().navigate(R.id.questionSuccess)
            //q = viewModel.currentQuestion.value!!
            //viewModel.setCurrentQuestion(q)

        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("lastCheckedId", lastCheckedId)
        val answers = arrayListOf<String>()
        for (view in answerViews) {
            val editText = view.findViewById<EditText>(R.id.answerEditText)
            answers.add(editText.text.toString())
        }
        outState.putStringArrayList("answers", answers)
    }

    private fun handleToggleSelection(checkedId: Int) {
        if (lastCheckedId == checkedId) {
            questionTypeToggleGroup.check(checkedId)
            return
        }

        when (checkedId) {
            R.id.btn_choice_1 -> {
                answersBlock.visibility = View.GONE
            }
            R.id.btn_choice_2 -> {
                answersBlock.visibility = View.VISIBLE
            }
        }

        lastCheckedId = checkedId
        questionTypeToggleGroup.check(checkedId)
    }

    private fun addAnswerField(answerText: String = "") {
        if (answerCount < 4) {
            val answerView = layoutInflater.inflate(R.layout.item_answer, answersContainer, false)

            val answerNumberTextView = answerView.findViewById<TextView>(R.id.answerNumberTextView)
            answerNumberTextView.text = (answerCount + 1).toString()

            val answerEditText = EditText(requireContext())
            answerEditText.id = View.generateViewId() // Устанавливаем уникальный ID для EditText
            Log.d("AddQuesFragment", "New EditText ID: ${answerEditText.id}")
            answerEditText.setText(answerText)

            val removeButton = answerView.findViewById<Button>(R.id.removeAnswerButton)
            removeButton.setOnClickListener {
                answersContainer.removeView(answerView)
                answerViews.remove(answerView)
                answerCount--
                updateAnswerNumbers()
            }

            answerViews.add(answerView)
            answersContainer.addView(answerView)
            answerCount++
        } else {
            Toast.makeText(requireContext(), "Максимально (4) ответа", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAnswerNumbers() {
        for (i in answerViews.indices) {
            val answerNumberTextView = answerViews[i].findViewById<TextView>(R.id.answerNumberTextView)
            answerNumberTextView.text = (i + 1).toString()
        }
    }

    private fun saveQuestion() {
        val question = questionEditText.text.toString()
        val answers = answerViews.mapNotNull {
            val editText = it.findViewById<EditText>(R.id.answerEditText)
            editText.text.toString().takeIf { text -> text.isNotBlank() }
        }

        if (question.isNotBlank() && answers.isNotEmpty()) {
            q = viewModel.createQuestionAndAnswers(MyApplication.currentUser!!.userID, question, answers)
            Toast.makeText(requireContext(), "Вопрос сохранен", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Введите вопрос и хотя бы один вариант ответа", Toast.LENGTH_SHORT).show()
        }
    }
}