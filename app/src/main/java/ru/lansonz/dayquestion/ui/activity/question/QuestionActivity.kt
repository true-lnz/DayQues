package ru.lansonz.dayquestion.ui.activity.question

import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gfg.article.customloadingbutton.LoadingButton
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.ActivityQuestionBinding
import ru.lansonz.dayquestion.databinding.ButtonAnswerBinding
import ru.lansonz.dayquestion.model.QuestionModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs


class QuestionActivity : AppCompatActivity() {
    private lateinit var questionViewModel: QuestionViewModel
    private lateinit var binding: ActivityQuestionBinding
    private lateinit var q: MutableLiveData<QuestionModel?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_question);
        setContentView(binding.root)

        questionViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance()).get(QuestionViewModel::class.java)

        binding.questionViewModel = questionViewModel
        binding.user = MyApplication.currentUser
        binding.lifecycleOwner = this

        hideSystemUI()

        val bg: ConstraintLayout  = findViewById(R.id.gradient_bg)
        val animDrawable: AnimationDrawable= bg.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(500)
        animDrawable.setExitFadeDuration(3000)
        animDrawable.start()

        q = questionViewModel.selectedQuestion

        q.observe(this, Observer { question ->
            question?.let {
                binding.questionText.text = it.text ?: "Ошибка получения вопроса"
                addAnswerButtons(it)
            }
        })
    }

    private fun addAnswerButtons(question: QuestionModel) {
        val answersContainer: LinearLayout = findViewById(R.id.answersContainer)
        answersContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val totalAnswers = question.answers.sumOf { it.ans_count }

        for ((index, answer) in question.answers.withIndex()) {
            val buttonBinding: ButtonAnswerBinding = DataBindingUtil.inflate(
                inflater, R.layout.button_answer, answersContainer, false)
            buttonBinding.answer = answer
            buttonBinding.progressPercentage = if (totalAnswers > 0) {
                answer.ans_count.toFloat() / totalAnswers
            } else { 1.0f }
            buttonBinding.root.tag = index // Установите индекс как тег
            answersContainer.addView(buttonBinding.root)
        }
    }

    var clicked = false
    fun onAnswerButtonClick(view: View) {
        if (view is LoadingButton) {
            if (clicked) return
            clicked = true

            val ansIndex = view.tag as Int
            view.progressColor = ContextCompat.getColor(this, R.color.colorAccent)
            view.rightText = (view.rightText.toInt() + 1).toString()
            if (!Prefs.getInstance(MyApplication.getInstance()).isNewUser) {
                questionViewModel.saveQuestionToHistory(q.value!!, MyApplication.currentUser!!.userID, ansIndex)
                Prefs.getInstance(MyApplication.getInstance()).saveQuestions(questionViewModel.cachedQuestions?.value!!)
            }

            // Запускаем анимации для остальных кнопок
            val answersContainer: LinearLayout = findViewById(R.id.answersContainer)
            for (i in 0 until answersContainer.childCount) {
                val child = answersContainer.getChildAt(i)
                if (child is LoadingButton && child != view) {
                    child.startAnimation() // Предполагается, что у вас есть метод для запуска анимации на вашей кнопке
                }
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    // Восстанавливаем полноэкранный режим при возобновлении активности
    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }


}
