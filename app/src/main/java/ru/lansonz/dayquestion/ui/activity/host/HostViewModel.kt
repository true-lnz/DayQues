package ru.lansonz.dayquestion.ui.activity.host

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HostViewModel : ViewModel() {
    private val _showNavigationBar = MutableLiveData<Boolean>().apply { value = false }

    val showNavigationBar: LiveData<Boolean>
        get() = _showNavigationBar

    fun showNavBar() {
        _showNavigationBar.value = true
    }

    fun hideNavBar() {
        _showNavigationBar.value = false
    }
}