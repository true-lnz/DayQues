package ru.lansonz.dayquestion.ui.activity.question

import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gfg.article.customloadingbutton.LoadingButton
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.ActivityQuestionBinding
import ru.lansonz.dayquestion.utils.MyApplication


class QuestionActivity : AppCompatActivity() {
    private lateinit var questionViewModel: QuestionViewModel
    private lateinit var binding: ActivityQuestionBinding

    private lateinit var loadingButton: LoadingButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_question);
        setContentView(binding.root)

        questionViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance()).get(QuestionViewModel::class.java)

        // Устанавливаем ViewModel и пользователя в binding
        binding.questionViewModel = questionViewModel
        binding.user = MyApplication.currentUser
        binding.lifecycleOwner = this

        // Устанавливаем полноэкранный режим
        hideSystemUI()

        val bg: ConstraintLayout  = findViewById(R.id.gradient_bg)
        val animDrawable: AnimationDrawable= bg.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(1500)
        animDrawable.setExitFadeDuration(3000)
        animDrawable.start()

        questionViewModel.selectedQuestion.observe(this, Observer { question ->
            question?.let {
                binding.questionText.text = it.text ?: "Ошибка получения вопроса"
            }
        })

        loadingButton = findViewById(R.id.custom_button1)

        var complete = false

        loadingButton.setOnClickListener {
            Toast.makeText(this,"File is downloading",Toast.LENGTH_LONG).show()
            loadingButton.hasCompletedDownload()

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
