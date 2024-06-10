package ru.lansonz.dayquestion.ui.fragment.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.FragmentProfileEditBinding
import ru.lansonz.dayquestion.utils.MyApplication


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

        viewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        binding.editTypeToggleGroup.check(R.id.btn_choice_1)
        binding.btnChoice1.setOnClickListener { handleToggleSelection(R.id.btn_choice_1) }
        binding.btnChoice2.setOnClickListener { handleToggleSelection(R.id.btn_choice_2) }

        binding.button.setOnClickListener {
            val snackBar = Snackbar.make(requireView(), "Удаление аккаунта пока недоступно", Snackbar.LENGTH_LONG)
            snackBar.show()
        }

        binding.btnVk.setOnClickListener { showEditDialog(binding.btnVk) }
        binding.btnTg.setOnClickListener { showEditDialog(binding.btnTg) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (MyApplication.currentUser?.fullName == "") {
            binding.socialBlock.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            if (MyApplication.currentUser?.fullName == "") {
                val snackBar = Snackbar.make(binding.root, "Ошибка: вы вошли как гость", Snackbar.LENGTH_LONG)
                snackBar.show()
            }

            viewModel.updateProfile(binding.etFullName.text.toString(), 
                                    binding.etAddress.text.toString(), 
                                    binding.etEmail.text.toString(),
/*                                    binding.btnVk.text.toString(),
                                    binding.btnTg.text.toString()*/
            )
            dismiss()
        }
    }

    var lastCheckedId = 0
    private fun handleToggleSelection(checkedId: Int) {
        if (lastCheckedId == checkedId) {
            binding.editTypeToggleGroup.check(checkedId)
            return
        }

        when (checkedId) {
            R.id.btn_choice_1 -> {
                binding.personal.visibility = View.VISIBLE
                binding.account.visibility = View.GONE
            }
            R.id.btn_choice_2 -> {
                binding.personal.visibility = View.GONE
                binding.account.visibility = View.VISIBLE
            }
        }

        lastCheckedId = checkedId
        binding.editTypeToggleGroup.check(checkedId)
    }

    private fun showEditDialog(button: Button) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_edit_text, null)
        builder.setView(dialogView)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        editText.setText(button.text.toString())
        builder.setPositiveButton("Готово") { dialog, which ->
            val newText = editText.text.toString()
            button.text = newText
        }
        builder.setNegativeButton("Отмена") { dialog, which -> dialog.dismiss() }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}