package ru.lansonz.dayquestion.ui.fragment.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ru.lansonz.dayquestion.model.NotificationModel
import ru.lansonz.dayquestion.utils.Prefs
import ru.lansonz.dayquestion.databinding.FragmentNotificationBinding
import ru.lansonz.dayquestion.decoration.RecyclerViewDecoration
import ru.lansonz.dayquestion.utils.MyApplication

class NotificationFragment : Fragment() {
    private lateinit var binding: FragmentNotificationBinding
    private lateinit var viewModel: NotificationViewModel
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(NotificationViewModel::class.java)
        binding = FragmentNotificationBinding.inflate(inflater, container, false)

        binding.notificationViewModel = viewModel
        binding.lifecycleOwner = this

        val prefs = Prefs.getInstance(requireContext())
        val notifications = prefs.getNotifications()

        if (notifications.isEmpty()) {
            binding.recyclerViewNotifications.visibility = View.GONE
            binding.noneNotifications.visibility = View.VISIBLE
        }

        notificationAdapter = NotificationAdapter(notifications)
        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
            addItemDecoration(RecyclerViewDecoration(32))
        }

        val currentUser = MyApplication.currentUser

        val newNotifications = if (currentUser?.fullName.isNullOrEmpty()) {
            listOf(
                NotificationModel("Вопрос Дня", "2 минуты назад", "Ваш сегодняшний Вопрос дня уже готов. Давайте посмотрим?", "Перейти"),
                NotificationModel("Вопрос Дня", "2 минуты назад", "Добро пожаловать в наше комьюнити!", "Перейти"),
                NotificationModel("Вопрос Дня", "3 минуты назад", "Вы вошли как гость!", "Перейти")
            )
        } else {
            listOf(
                NotificationModel("Вопрос Дня", "2 минуты назад", "Ваш сегодняшний Вопрос дня уже готов. Давайте посмотрим?", "Перейти"),
                NotificationModel("Вопрос Дня", "10 минут назад", "Ваш вопрос был успешно опубликован для публичного просмотра", "Просмотр"),
                NotificationModel("Вопрос Дня", "18 минут назад", "Завершите заполнения профиля: вы не заполнили еще социальные сети :", "Перейти"),
                NotificationModel("Роман Финогентов", "2 дня назад", "У пользователя новый вопрос. Можно уже смотреть!", "Просмотр"),
                NotificationModel("Вопрос Дня", "3 дня назад", "Ваш сегодняшний Вопрос дня уже готов. Давайте посмотрим?", "Перейти"),
                NotificationModel("Алексей Кузнецов", "3 дня назад", "У пользователя новый вопрос. Можно уже смотреть!", "Просмотр"),
                NotificationModel("Вопрос Дня", "4 минут назад", "Ваш вопрос был успешно опубликован для публичного просмотра", "Просмотр"),
                NotificationModel("Анна Петрова", "4 дня назад", "У пользователя новый вопрос. Можно уже смотреть!", "Просмотр"),
                NotificationModel("Вопрос Дня", "4 дня назад", "Ваш сегодняшний Вопрос дня уже готов. Давайте посмотрим?", "Перейти"),
                NotificationModel("Вопрос Дня", "5 дней назад", "Добро пожаловать в наше комьюнити!", "Перейти"),
            )
        }

        // Сохраняем уведомления
        Prefs.getInstance(MyApplication.getInstance()).saveNotifications(newNotifications)

        // Отправляем уведомления
        newNotifications.forEach { notification ->
            if (currentUser?.fullName.isNullOrEmpty())
                NotificationUtils.sendNotification(requireContext(), notification.userName, notification.message)
        }

        NotificationUtils.sendNotification(requireContext(), newNotifications.get(0).userName, newNotifications.get(0).message)
        NotificationUtils.sendNotification(requireContext(), newNotifications.get(2).userName, newNotifications.get(0).message)

        return binding.root
    }
}
