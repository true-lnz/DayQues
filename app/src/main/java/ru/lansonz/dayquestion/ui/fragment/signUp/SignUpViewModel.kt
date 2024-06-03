package ru.lansonz.dayquestion.ui.fragment.signUp

import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import ru.lansonz.dayquestion.model.UserModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.utils.Prefs

class SignUpViewModel(private val myApplication: Application) : AndroidViewModel(myApplication) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    var fullName: String = ""
    var address: String = ""
    var email: String = ""
    var password: String = ""
    var imageUri: Uri? = null

    private val _fullNameError = MutableLiveData<String>()
    val fullNameError: LiveData<String>
        get() = _fullNameError

    private val _emailError = MutableLiveData<String>()
    val emailError: LiveData<String>
        get() = _emailError

    private val _passwordError = MutableLiveData<String>()
    val passwordError: LiveData<String>
        get() = _passwordError

    private val _buttonEnabled = MutableLiveData<Boolean>().apply { value = true }
    val buttonEnabled: LiveData<Boolean>
        get() = _buttonEnabled

    private val _navigateToHome = MutableLiveData<Boolean>().apply { value = false }
    val navigateToHome: LiveData<Boolean>
        get() = _navigateToHome

    private val _navigateToBack = MutableLiveData<Boolean>().apply { value = false }
    val navigateToBack: LiveData<Boolean>
        get() = _navigateToBack

    private val _navigateToForward = MutableLiveData<Boolean>().apply { value = false }
    val navigateToForward: LiveData<Boolean>
        get() = _navigateToForward

    private val _navigateToError = MutableLiveData<Boolean>().apply { value = false }
    val navigateToError: LiveData<Boolean>
        get() = _navigateToError

    private val _signUpError = MutableLiveData<String>()
    val signUpError: LiveData<String>
        get() = _signUpError

    fun createUser() {
        _buttonEnabled.value = false
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener { authResult ->
            val userId = authResult.user?.uid
            if (userId != null) {
                if (imageUri == null) {
                    createUserWithoutImage(userId)
                } else {
                    createUserWithImage(userId)
                }
            }
        }.addOnFailureListener {
            _signUpError.value = it.message
            _buttonEnabled.value = true
            _navigateToError.value = true
        }
    }

    private fun createUserWithoutImage(userId: String) {
        val userRef: DatabaseReference = database.reference.child("users").child(userId)
        val userModel = UserModel(userId, email, fullName, address, "", true)
        userRef.setValue(userModel).addOnSuccessListener {
            MyApplication.currentUser = userModel
            Prefs.getInstance(MyApplication.getInstance()).saveUser(userModel)
            _navigateToHome.value = true
        }.addOnFailureListener {
            _signUpError.value = it.message
            _buttonEnabled.value = true
            _navigateToError.value = true
        }
    }

    private fun createUserWithImage(userId: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images").child("$userId.png")
        storageRef.putFile(imageUri!!).addOnSuccessListener { taskSnapshot ->
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val userModel = UserModel(userId, email, fullName, address, uri.toString(), true, true)
                val userRef: DatabaseReference = database.reference.child("users").child(userId)
                userRef.setValue(userModel).addOnSuccessListener {
                    MyApplication.currentUser = userModel
                    Prefs.getInstance(MyApplication.getInstance()).saveUser(userModel)
                    _navigateToHome.value = true
                }.addOnFailureListener {
                    _signUpError.value = it.message
                    _buttonEnabled.value = true
                    _navigateToError.value = true
                }
            }.addOnFailureListener {
                _signUpError.value = it.message
                _buttonEnabled.value = true
                _navigateToError.value = true
            }
        }.addOnFailureListener {
            _signUpError.value = it.message
            _buttonEnabled.value = true
            _navigateToError.value = true
        }
    }

    fun forwardNavigating() {
        when {
            fullName.isEmpty() -> _passwordError.value = myApplication.getString(R.string.name_required_error)
            email.isEmpty() -> _emailError.value = myApplication.getString(R.string.email_required_error)
            password.isEmpty() -> _passwordError.value = myApplication.getString(R.string.password_required_error)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> _emailError.value = myApplication.getString(R.string.malformed_email_error)
            password.length < 6 -> _passwordError.value = myApplication.getString(R.string.short_password_error)
            else -> {
                _navigateToForward.value = true
            }
        }
    }

    fun backNavigating() {
        _navigateToBack.value = true
    }

    fun doneForwardNavigating() {
        _navigateToForward.value = false
    }

    fun doneBackNavigating() {
        _navigateToBack.value = false
    }

    fun doneNavigating() {
        _navigateToHome.value = false
    }

    fun doneError() {
        _navigateToError.value = false
    }
}
