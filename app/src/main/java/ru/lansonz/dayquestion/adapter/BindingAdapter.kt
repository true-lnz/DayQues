package ru.lansonz.dayquestion.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.gfg.article.customloadingbutton.LoadingButton
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import ru.lansonz.dayquestion.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@BindingAdapter("hideIfFalse")
fun hideIfFalse(view: View, boolean: Boolean) {
    if (boolean) view.visibility = View.VISIBLE else view.visibility = View.GONE
}

@BindingAdapter("hideIfEmpty")
fun hideIfEmpty(textView: TextView, error: String?) {
    if (error != null) {
        if (error.isEmpty()) {
            textView.visibility = View.INVISIBLE
        } else {
            textView.visibility = View.VISIBLE
            textView.text = error
        }
    }
}

@BindingAdapter("setAddress")
fun setAddress(textView: TextView, address: String) {
    if (address == "") {
        textView.text = "Не указано"
    } else {
        textView.text = address
    }
}

@BindingAdapter("setImage")
fun setImage(imageView: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        val picasso = Picasso.get()
        picasso.load(url)
            .placeholder(R.drawable.placeholder) // Изображение-заполнитель
            //.networkPolicy(NetworkPolicy.OFFLINE)
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    // Изображение успешно загружено
                }

                override fun onError(e: Exception?) {
                    // Ошибка загрузки изображения из кэша, пробуем загрузить из сети
                    picasso.load(url)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder) // Используется в случае ошибки
                        .networkPolicy(NetworkPolicy.NO_CACHE) // Пропустить кэширование
                        .into(imageView)
                }
            })
    } else {
        imageView.setImageResource(R.drawable.placeholder)
    }
}

@BindingAdapter("convertTimestampToDateString")
fun convertTimestampToDateString(view: TextView, timestamp: Long?) {
    timestamp?.let {
        val date = Date(it)
        val calendar = Calendar.getInstance()
        calendar.time = date

        val todayCalendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("d MMMM HH:mm", Locale.getDefault())

        val dayDifference = todayCalendar.get(Calendar.DAY_OF_YEAR) - calendar.get(Calendar.DAY_OF_YEAR)
        val dayOfWeek = dayFormat.format(date).capitalize(Locale.getDefault())
        val formattedDate = when {
            dayDifference == 0 -> "Сегодня, ${dateFormat.format(date)}"
            dayDifference == 1 -> "Вчера, ${dateFormat.format(date)}"
            else -> "$dayOfWeek, ${dateFormat.format(date)}"
        }

        view.text = formattedDate
    }
}

@BindingAdapter("formattedNumber")
fun setFormattedNumber(textView: TextView, number: Int) {
    val formattedNumber: String
    formattedNumber = if (number >= 1000) {
        String.format("%.1fK", number / 1000.0)
    } else {
        number.toString()
    }
    textView.text = formattedNumber
}

@BindingAdapter("setProgressPercentage")
fun setProgressPercentage(view: LoadingButton, progress: Float) {
    view.progressPercentageValue = progress
}

@BindingAdapter("getWelcomeWord")
fun getWelcomeWord(textView: TextView, fullName: String?) {
    fullName?.let {
        val firstWord = fullName.split(" ").firstOrNull()
        textView.text = String.format("Привет, %s", firstWord)
    }
}