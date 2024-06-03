package ru.lansonz.dayquestion.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.FragmentProfileEditBinding

class ProfileEdit : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentProfileEditBinding
    private lateinit var viewModel: ProfileViewModel

    companion object {
        const val TAG = "ProfileEdit"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.user = MyApplication.currentUser
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSave.setOnClickListener {
            viewModel.updateProfile(binding.etFullName.text.toString(), 
                                    binding.etAddress.text.toString(), 
                                    binding.etEmail.text.toString(), null)
            // TODO: реализовать обновление картинки
        }
    }

}