package ru.lansonz.dayquestion.ui.fragment.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.VKApiManager
import com.vk.api.sdk.auth.VKAuthenticationResult
import com.vk.api.sdk.auth.VKScope
import com.vk.dto.common.id.UserId
import com.vk.sdk.api.friends.dto.FriendsGetFieldsResponseDto
import com.vk.sdk.api.users.UsersService
import com.vk.sdk.api.users.dto.UsersFieldsDto
import com.vk.sdk.api.users.dto.UsersRelativeDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import ru.lansonz.dayquestion.ui.activity.host.HostActivity
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private val RC_SIGN_IN = 1
    private val VK_SIGN_IN = 2

    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginViewModel

    private lateinit var authLauncher: ActivityResultLauncher<Collection<VKScope>>
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.loginViewModel = viewModel
        binding.lifecycleOwner = this

        setupListeners() // listeners
        setUpObservers() // To home activity

        authLauncher = VK.login(activity as HostActivity) { result : VKAuthenticationResult ->
            when (result) {
                is VKAuthenticationResult.Success -> {
                    val vkId = result.token.userId
                    val vkEmail = result.token.email
                    val vkAccessToken = result.token.accessToken
                    VK.execute(
                        UsersService().usersGet(userIds = listOf(result.token.userId)),
                        object : VKApiCallback<List<UsersUserFullDto>> {
                            override fun success(result: List<UsersUserFullDto>) {
                                val user = result.get(0)
                                user?.let {
                                    // Вызов авторизации по ВК
                                    val vkFullName = it.firstName + " " +  it.lastName
                                    viewModel.firebaseAuthWithVK(vkId, vkEmail, vkFullName, vkAccessToken)
                                }
                            }
                            override fun fail(error: Exception) {
                                viewModel._errorString.value = "Не удалось авторизоваться [2]"
                            }
                        })
                }
                is VKAuthenticationResult.Failed -> {
                    viewModel._errorString.value = "Не удалось авторизоваться [3]"
                }
            }
        }

        return binding.root
    }

    private fun setupListeners() {
        binding.etEmail.doAfterTextChanged { email -> viewModel.username = email.toString() }   // запомнить
        binding.etPassword.doAfterTextChanged { pass -> viewModel.password = pass.toString() }  // через viewModel
        binding.googleSignIn.setOnClickListener {
            signInWithGoogle()
        }
        binding.vkSignIn.setOnClickListener {
            signInWithVK()
        }
    }

    private fun setUpObservers() {
        viewModel.navigateToHome.observe(viewLifecycleOwner, Observer {
            if (it) {
                startActivity(Intent(context, HostActivity::class.java))
                (activity as HostActivity).finish()
                viewModel.doneHomeNavigation()
            }
        })
        viewModel.navigateToSignUp.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
                viewModel.doneSignUpNavigation()
            }
        })
        viewModel.errorString.observe(viewLifecycleOwner, Observer {
            it?.let { errorMessage ->
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
                val snackBar = Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG)
                snackBar.show()
            }
        })
    }

    private fun signInWithVK() {
        authLauncher.launch(arrayListOf(VKScope.EMAIL, VKScope.WALL, VKScope.PHOTOS, VKScope.PHONE, VKScope.STATS))
    }

    private fun signInWithGoogle() {
        val signInIntent = (activity as HostActivity).googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                e.printStackTrace()
                viewModel._errorString.value = e.message
            }
        }
    }
}
