package ru.lansonz.dayquestion.ui.fragment.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class NotificationViewModel : ViewModel() {
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    var username: String = ""
        set(value) {
            field = value
            validateInput()
        }

    var password: String = ""
        set(value) {
            field = value
            validateInput()
        }

    private val _buttonEnabled = MutableLiveData<Boolean>()
    val buttonEnabled: LiveData<Boolean>
        get() = _buttonEnabled

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean>
        get() = _progress

    private val _errorString = MutableLiveData<String>()
    val errorString: LiveData<String>
        get() = _errorString

    private fun validateInput() {
        _buttonEnabled.value = !(username.isEmpty() || password.isEmpty())
    }

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean>
        get() = _navigateToHome

    private val _navigateToSignUp = MutableLiveData<Boolean>()
    val navigateToSignUp: LiveData<Boolean>
        get() = _navigateToSignUp


    fun navigateToSignUp() {
        _navigateToSignUp.value = true
    }

    fun doneSignUpNavigation() {
        _navigateToSignUp.value = false
    }

    private fun startHomeNavigation() {
        _navigateToHome.value = true
    }

    fun doneHomeNavigation() {
        _navigateToHome.value = false
    }

    private fun onStartLoading() {
        _buttonEnabled.value = false
        _progress.value = true
    }

    private fun onFinishLoading() {
        _buttonEnabled.value = true
        _progress.value = false
    }

}