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


/*        val newNotifications = listOf(
            NotificationModel("Roman Kamushken", "2 days ago", "Check out awesome updates for Android design kit for Figma. Now more screens and more categories.", "117 REPLAYS"),
            NotificationModel("Tina Turbina", "18 minutes ago", "into component container and with resizing constraints set????", "117 REPLAYS"),
            NotificationModel("Tomek Kuwalskij", "2 days ago", "into component container and with resizing constraints set????", "117 REPLAYS"),
            NotificationModel("Roman Kamushken", "2 days ago", "Check out awesome updates for Android design kit for Figma. Now more screens and more categories.", "117 REPLAYS"),
            NotificationModel("Tina Turbina", "18 minutes ago", "into component container and with resizing constraints set????", "117 REPLAYS"),
            NotificationModel("Tomek Kuwalskij", "2 days ago", "into component container and with resizing constraints set????", "117 REPLAYS")
        )
        prefs.saveNotifications(newNotifications)*/

        return binding.root
    }
}
