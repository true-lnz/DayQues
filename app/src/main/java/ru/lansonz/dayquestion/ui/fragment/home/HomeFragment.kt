package ru.lansonz.dayquestion.ui.fragment.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.adapter.QuestionAdapter
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel
import ru.lansonz.dayquestion.databinding.FragmentHomeBinding
import ru.lansonz.dayquestion.ui.activity.question.ViewModelFactory
import ru.lansonz.dayquestion.utils.MyApplication

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var questionViewModel: QuestionViewModel
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var myApplication: MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myApplication = MyApplication.getInstance()
        if (MyApplication.currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        questionViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance()).get(QuestionViewModel::class.java)
        binding.questionViewModel = questionViewModel
        binding.user = MyApplication.currentUser
        binding.lifecycleOwner = this

        // Настройка RecyclerView
        val adapter = QuestionAdapter(emptyList())
        binding.questionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.questionsRecyclerView.adapter = adapter

        // Загружаем кэшированные вопросы
        questionViewModel.loadCachedQuestions()

        // Получаем вопросы для пользователя и сохраняем их в кэш
        questionViewModel.getQuestions(mAuth.currentUser!!.uid)

        // Наблюдаем за изменениями в списке кэшированных вопросов и новых вопросов пользователя
        questionViewModel.cachedQuestions.observe(viewLifecycleOwner, Observer { questions ->
            questions?.let {
                adapter.updateQuestions(it)
            }
        })

        binding.btnQues.setOnClickListener {
            questionViewModel.getUniqueOrOldestQuestion(mAuth.currentUser!!.uid)
            findNavController().navigate(R.id.questionActivity)
        }

        return binding.root
    }
}
