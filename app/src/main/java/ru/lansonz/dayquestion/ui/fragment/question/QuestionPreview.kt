package ru.lansonz.dayquestion.ui.fragment.question

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.FragmentProfileBinding
import ru.lansonz.dayquestion.databinding.FragmentQuesPreviewBinding
import ru.lansonz.dayquestion.ui.activity.host.HostViewModel
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel
import ru.lansonz.dayquestion.ui.fragment.profile.ProfileViewModel
import ru.lansonz.dayquestion.utils.MyApplication

class QuestionPreview : Fragment() {
    private lateinit var viewModel: QuestionViewModel
    private lateinit var binding: FragmentQuesPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(QuestionViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQuesPreviewBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.question = viewModel.currentQuestion.value

        return binding.root
    }
}
