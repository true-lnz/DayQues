package ru.lansonz.dayquestion.ui.fragment.profile

import android.Manifest
import android.app.Activity
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
import androidx.fragment.app.DialogFragment.STYLE_NORMAL
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.FragmentProfileBinding
import ru.lansonz.dayquestion.ui.activity.host.HostActivity
import ru.lansonz.dayquestion.ui.activity.host.HostViewModel
import ru.lansonz.dayquestion.utils.Prefs

class ProfileFragment : Fragment() {

    private val PERMISSION_CODE = 1
    private val PICK_IMAGE_CODE = 2
    private var imageUri: Uri? = null
    private lateinit var binding: FragmentProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var hostViewModel: HostViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        hostViewModel = ViewModelProvider(this).get(HostViewModel::class.java)

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
        binding.btnSettings.setOnClickListener {
        }



        return binding.root
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

