package ru.lansonz.dayquestion.ui.fragment.signUp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        binding.signUpViewModel = viewModel
        binding.lifecycleOwner = this
        setupListeners()
        setupObservers()

        return binding.root
    }

    private fun setupListeners() {
        binding.etFullName.doAfterTextChanged { fullname ->
            viewModel.fullName = fullname.toString()
        }
        binding.etEmail.doAfterTextChanged { email ->
            viewModel.email = email.toString()
        }
        binding.etPassword.doAfterTextChanged { password ->
            viewModel.password = password.toString()
        }
    }

    private fun setupObservers() {
        viewModel.fullNameError.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                binding.etFullName.error = it
            }
        })
        viewModel.emailError.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                binding.etEmail.error = it
            }
        })
        viewModel.passwordError.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                binding.etPassword.error = it
            }
        })
        viewModel.navigateToForward.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigate(R.id.action_signUpFragment_to_signUpAvatar)
                viewModel.doneForwardNavigating()
            }
        })
        viewModel.navigateToBack.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigate(R.id.action_signUpFragment_to_authFragment)
                viewModel.doneBackNavigating()
            }
        })
    }
}