package ru.lansonz.dayquestion.ui.fragment.home

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.adapter.QuestionAdapter
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel
import ru.lansonz.dayquestion.databinding.FragmentHomeBinding
import ru.lansonz.dayquestion.decoration.RecyclerViewDecoration
import ru.lansonz.dayquestion.ui.activity.question.ViewModelFactory
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var questionViewModel: QuestionViewModel
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        questionViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance()).get(QuestionViewModel::class.java)
        binding.questionViewModel = questionViewModel
        binding.user = Prefs.getInstance(MyApplication.getInstance()).getUser()
        binding.lifecycleOwner = this

        val adapter = QuestionAdapter(emptyList())
        binding.questionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.questionsRecyclerView.adapter = adapter
        binding.questionsRecyclerView.addItemDecoration(RecyclerViewDecoration(32))

        questionViewModel.clearCachedQuestions()
        questionViewModel.getLastQuestions(MyApplication.currentUser?.userID ?: "")

        // Загружаем кэшированные вопросы
        if (Prefs.getInstance(MyApplication.getInstance()).isNewUser) {
            binding.questionsRecyclerView.visibility = View.GONE
            binding.newUserBlock.visibility = View.VISIBLE
            Prefs.getInstance(MyApplication.getInstance()).isNewUser = false
            if (MyApplication.currentUser?.fullName == "") {
                binding.tvHistoryPlug.text = "История доступна только для зарегистрированных пользователей"
            }
        } else {
            questionViewModel.loadCachedQuestions()
            binding.pointsTextView.text = "127"
            binding.rankingTextView.text = "48"
        }

        // Наблюдаем за изменениями в списке кэшированных вопросов и новых вопросов пользователя
        questionViewModel.cachedQuestions.observe(viewLifecycleOwner, Observer { questions ->
            questions?.let {
                adapter.updateQuestions(it)
                Log.d("tag", "$it")
            }
        })

        binding.dayQues.setOnClickListener {
            questionViewModel.getUniqueOrOldestQuestion(mAuth.currentUser!!.uid)
            findNavController().navigate(R.id.questionActivity)
        }

        binding.btnFavorites.setOnClickListener {
            val snackBar = Snackbar.make(binding.root, "Функционал Подписок не реализован.", Snackbar.LENGTH_LONG)
            snackBar.show()
        }

        return binding.root
    }
}
