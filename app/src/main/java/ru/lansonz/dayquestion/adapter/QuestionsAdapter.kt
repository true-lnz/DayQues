package ru.lansonz.dayquestion.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.ItemQuestionBinding
import ru.lansonz.dayquestion.model.QuestionModel

class QuestionsAdapter(private val questions: List<QuestionModel>) :
    RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ItemQuestionBinding>(
            inflater, R.layout.item_question, parent, false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        holder.binding.question = question
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = questions.size
}
