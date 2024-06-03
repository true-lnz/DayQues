package ru.lansonz.dayquestion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.lansonz.dayquestion.model.QuestionModel
import ru.lansonz.dayquestion.databinding.ItemQuestionBinding

class QuestionAdapter(private var questions: List<QuestionModel>) :
    RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount() = questions.size

    fun updateQuestions(newQuestions: List<QuestionModel>) {
        questions = newQuestions
        notifyDataSetChanged()
    }

    class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuestionModel) {
            binding.question = question
            binding.executePendingBindings()
        }
    }
}
