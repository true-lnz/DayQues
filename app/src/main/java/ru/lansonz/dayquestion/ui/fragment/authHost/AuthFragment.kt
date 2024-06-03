package ru.lansonz.dayquestion.ui.fragment.authHost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import ru.lansonz.dayquestion.model.UserModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.databinding.FragmentAuthBinding

class AuthFragment : Fragment() {

    private lateinit var binding: FragmentAuthBinding
    private lateinit var viewModel: AuthViewModel
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        binding = FragmentAuthBinding.inflate(inflater, container, false)
        binding.authViewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.navigateToLogin.observe(viewLifecycleOwner, Observer {
            if (it) {
                this.findNavController()
                    .navigate(AuthFragmentDirections.actionAuthFragmentToLoginFragment())
                viewModel.doneNavigationToLogin()
            }
        })
        viewModel.navigateToSkip.observe(viewLifecycleOwner, Observer {
            mAuth.signInAnonymously()
                .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    val user = UserModel(mAuth.currentUser!!.uid)
                    MyApplication.currentUser = user
                } else {
                    Toast.makeText(
                        context,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            if (it) {
/*                this.findNavController()
                    .navigate(AuthFragmentDirections.actionHome())*/
                viewModel.doneNavigationToSkip()
            }
        })

        return binding.root
    }
}