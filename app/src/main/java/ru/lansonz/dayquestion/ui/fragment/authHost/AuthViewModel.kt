package ru.lansonz.dayquestion.ui.fragment.authHost

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {
    private val _navigateToLogin = MutableLiveData<Boolean>().apply { value = false }
    val navigateToLogin: LiveData<Boolean>
        get() = _navigateToLogin

    private val _navigateToSkip = MutableLiveData<Boolean>().apply { value = false }
    val navigateToSkip: LiveData<Boolean>
        get() = _navigateToSkip

    fun startNavigationToLogin() {
        _navigateToLogin.value = true
    }

    fun doneNavigationToLogin() {
        _navigateToLogin.value = false
    }

    fun navigateToSkip() {
        _navigateToSkip.value = true
    }

    fun doneNavigationToSkip() {
        _navigateToSkip.value = false
    }
}