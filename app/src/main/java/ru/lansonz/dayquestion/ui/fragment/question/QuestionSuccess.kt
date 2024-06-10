package ru.lansonz.dayquestion.ui.fragment.question

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel

class QuestionSuccess : Fragment() {
    private lateinit var viewModel: QuestionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(QuestionViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ques_success, container, false)

        view.findViewById<Button>(R.id.btn_preview).setOnClickListener {
            findNavController().navigate(R.id.profileFragment)

        }

        return view
    }
}