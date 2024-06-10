package ru.lansonz.dayquestion.ui.fragment.profile

import android.Manifest
import android.R.attr.label
import android.R.attr.text
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.DialogFragment.STYLE_NORMAL
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.vk.api.sdk.VK
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.adapter.QuestionAdapter
import ru.lansonz.dayquestion.adapter.QuestionsAdapter
import ru.lansonz.dayquestion.databinding.FragmentProfileBinding
import ru.lansonz.dayquestion.model.NotificationModel
import ru.lansonz.dayquestion.ui.activity.host.HostViewModel
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel
import ru.lansonz.dayquestion.ui.activity.question.ViewModelFactory
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs
import ru.lansonz.dayquestion.utils.RealtimeDatabaseUtil


class ProfileFragment : Fragment() {

    private val PERMISSION_CODE = 1
    private val PICK_IMAGE_CODE = 2
    private var imageUri: Uri? = null
    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: FragmentProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var hostViewModel: HostViewModel
    private lateinit var quesViewModel: QuestionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        hostViewModel = ViewModelProvider(this).get(HostViewModel::class.java)
        quesViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance()).get(QuestionViewModel::class.java)

        binding.lifecycleOwner = this
        binding.user = MyApplication.currentUser
        binding.profileViewModel = viewModel

        binding.btnEdit.setOnClickListener {
            ProfileEdit().setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
            ProfileEdit().show(childFragmentManager, ProfileEdit.TAG)
        }

        binding.profileImage.setOnClickListener { view ->
            showPopupMenu(view)
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.btnSettings.setOnClickListener {
            MyApplication.currentUser!!.active = false
            RealtimeDatabaseUtil.updateUser(MyApplication.currentUser!!) {
                mAuth?.signOut()
            }
            googleSignInClient.signOut()
            VK.logout()
            MyApplication.currentUser = null
            Prefs.getInstance(MyApplication.getInstance()).clearAll()
            findNavController().navigate(R.id.action_logout)
        }

        binding.btnVk.setOnClickListener {
            // Получим текст кнопки
            val textToCopy = binding.btnVk.text.toString()

            val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager

            // Создадим объект ClipData с текстом для копирования
            val clip = ClipData.newPlainText("simple text", textToCopy)

            // Скопируем текст в буфер обмена
            clipboard.setPrimaryClip(clip)

            // Покажем уведомление пользователю
            Toast.makeText(requireContext(), "Cкопировано в буфер обмена", Toast.LENGTH_SHORT).show()
        }

        binding.btnTg.setOnClickListener {
            // Получим текст кнопки
            val textToCopy = binding.btnTg.text.toString()

            val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager

            // Создадим объект ClipData с текстом для копирования
            val clip = ClipData.newPlainText("simple text", textToCopy)

            // Скопируем текст в буфер обмена
            clipboard.setPrimaryClip(clip)

            // Покажем уведомление пользователю
            Toast.makeText(requireContext(), "Cкопировано в буфер обмена", Toast.LENGTH_SHORT).show()
        }

        if (MyApplication.currentUser?.fullName == "") {
            binding.socialBlock.visibility = View.GONE
            binding.questionsBlock.visibility = View.GONE
            binding.guestUserLayout.visibility = View.VISIBLE
        }

        if (Prefs.getInstance(MyApplication.getInstance()).isNewUser) {
            Prefs.getInstance(MyApplication.getInstance()).isNewUser = false
            binding.textView11.text = "0 очков"
            binding.textView12.text = "0 вопросов"
            binding.textView13.text = "0 ответов"
        }

        binding.btnGoToSignUp.setOnClickListener {
            Prefs.getInstance(MyApplication.getInstance()).clearAll()
            findNavController().navigate(R.id.authFragment)
        }

        if (MyApplication.currentUser?.fullName != "") {
            quesViewModel.getUserQuestions(MyApplication.currentUser?.userID!!)
            setupRecyclerView()

        }

        return binding.root
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.questionsRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        quesViewModel.questions.observe(viewLifecycleOwner, Observer { questions ->
            if (questions != null) {
                val adapter = QuestionsAdapter(questions)
                recyclerView.adapter = adapter
            }
        })
    }
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view, 1, 0, R.style.CustomPopupMenu)
        popup.menuInflater.inflate(R.menu.profile_picture_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.edit_photo -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        checkPermissions()
                    } else {
                        pickFromGallery()
                    }
                    true
                }
                R.id.delete_photo -> {
                    viewModel.deleteProfilePicture()
                    Picasso.get().load("https://hello.ru")
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE, NetworkPolicy.OFFLINE)
                        .into(binding.profileImage);
                    Toast.makeText(requireContext(), "Фото профиля удалено", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery()
                }
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_CODE
            )
        } else {
            pickFromGallery()
        }
    }

    private fun pickFromGallery() {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"

        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.type = "image/*"

        val chooserIntent = Intent.createChooser(getIntent, getString(R.string.select_image))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        startActivityForResult(chooserIntent, PICK_IMAGE_CODE)
    }

    val mAuth: FirebaseAuth? = FirebaseAuth.getInstance()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageUri = data.data
                Picasso.get().load(imageUri)
                    .placeholder(R.drawable.placeholder)
                    .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE, NetworkPolicy.OFFLINE)
                    .into(binding.profileImage);
                viewModel.updateProfilePicture(imageUri!!)
                Prefs.getInstance(MyApplication.getInstance()).saveUser(MyApplication.currentUser!!)
                Toast.makeText(requireContext(), "Фото профиля обновлено", Toast.LENGTH_SHORT).show()
                Log.d("TAG1", "mAuth aдрес $mAuth.currentUser?.profilePictureURL")
                Log.d("TAG1", "prefs адрес ${Prefs.getInstance(MyApplication.getInstance()).getUser()?.profilePictureURL}")
                Log.d("TAG1", "MyApp адрес ${MyApplication.currentUser?.profilePictureURL}")
            }
            else
                Toast.makeText(requireContext(), "Отменено", Toast.LENGTH_SHORT).show()

        }
    }
}

