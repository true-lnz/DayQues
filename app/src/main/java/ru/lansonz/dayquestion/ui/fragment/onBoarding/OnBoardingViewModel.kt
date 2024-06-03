package ru.lansonz.dayquestion.ui.fragment.onBoarding

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import ru.lansonz.dayquestion.model.SlideContent
import ru.lansonz.dayquestion.R

class OnBoardingViewModel(application: Application) : AndroidViewModel(application) {
    private val list = listOf(
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_welcome)!!,
            "Добро пожаловать!",
            "Начните свой день с интересного вопроса. Поможем вам настроиться на позитивный лад"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_question)!!,
            "Отвечайте на вопросы",
            "Каждый день вы получаете новый вопрос. Делитесь свой точкой зрения\nи читайте, что думают другие"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_star)!!,
            "Оценивайте ответы",
            "Понравился чей-то ответ? Оцените его! Так вы помогаете выделится лучшим идеям"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_account)!!,
            "Создайте свой профиль",
            "Зарегистрируйтесь, чтобы задавать вопросы. Вы сможете указать свои соцсети"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_community)!!,
        "Станьте частью сообщества",
        "Вопрос Дня - это больше, чем ответы на вопросы. Это место для поддержки и вдохновения"
    )
    )

    private val _dataSet = MutableLiveData<List<SlideContent>>().apply { value = list }
    val dataSet: LiveData<List<SlideContent>>
        get() = _dataSet

    private val _buttonVisiability = MutableLiveData<Boolean>().apply { value = false }
    val buttonVisiability: LiveData<Boolean>
        get() = _buttonVisiability

    val pagerCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            _buttonVisiability.value = position == list.size - 1
            super.onPageSelected(position)
        }
    }
    private val _startNavigation = MutableLiveData<Boolean>().apply { value = false }
    val startNavigation: LiveData<Boolean>
        get() = _startNavigation

    fun navigateToAuth() {
        _startNavigation.value = true
    }

    fun doneNavigation() {
        _startNavigation.value = false
    }
}