package ru.lansonz.dayquestion.ui.fragment.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.dto.common.id.UserId
import com.vk.dto.common.id.toUserId
import ru.lansonz.dayquestion.model.UserModel
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs
import ru.lansonz.dayquestion.utils.RealtimeDatabaseUtil

class LoginViewModel : ViewModel() {
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var dbReference: DatabaseReference = database.reference
    private var prefs = Prefs.getInstance(MyApplication.getInstance())

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

    val _errorString = MutableLiveData<String>()
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

    fun login() {
        onStartLoading()
        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener {
            if (it.isSuccessful) {
                getFirebaseUserData()
            } else {
                onFinishLoading()
                _errorString.value = it.exception?.message
            }
        }
    }

    private fun getFirebaseUserData() {
        val userId = mAuth.currentUser!!.uid
        val userRef = dbReference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userInfo = dataSnapshot.getValue(UserModel::class.java)
                    MyApplication.currentUser = userInfo
                    MyApplication.currentUser!!.active = true
                    prefs.saveUser(userInfo!!)
                    RealtimeDatabaseUtil.updateUser(MyApplication.currentUser!!) {}
                    onFinishLoading()
                    startHomeNavigation()
                } else {
                    onFinishLoading()
                    _errorString.value = "Данные пользователя не найдены."
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                onFinishLoading()
                _errorString.value = databaseError.message
            }
        })
    }

    private fun saveSocialUserToFirebase(
        currentUser: FirebaseUser?,
        items: java.util.HashMap<String, Any>
    ) {
        val userId = currentUser!!.uid
        val userRef = dbReference.child("users").child(userId)

        userRef.setValue(items)
            .addOnSuccessListener {
                val userInfo = UserModel()
                userInfo.userID = userId
                userInfo.email = items["email"].toString()
                userInfo.fullName = items["fullName"].toString()
                userInfo.address = items["address"].toString()
                userInfo.profilePictureURL = items["profilePictureURL"].toString()
                userInfo.active = true
                MyApplication.currentUser = userInfo

                onFinishLoading()
                startHomeNavigation()
            }
            .addOnFailureListener { e ->
                _errorString.value = e.message
                onFinishLoading()
            }
    }

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

    fun navigateToRecovery() {
        _errorString.value = "Восстановление пароля недоступно."
    }

    private fun onStartLoading() {
        _buttonEnabled.value = false
        _progress.value = true
    }

    private fun onFinishLoading() {
        _buttonEnabled.value = true
        _progress.value = false
    }

    fun setError(error: String) {
        _errorString.value = error
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        onStartLoading()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = mAuth.currentUser!!.uid
                val userRef = dbReference.child("users").child(userId)

                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val document = dataSnapshot.getValue(UserModel::class.java)
                            if (document != null) {
                                MyApplication.currentUser = document
                                prefs.saveUser(document)
                                RealtimeDatabaseUtil.updateUser(MyApplication.currentUser!!) {}
                                startHomeNavigation()
                            } else {
                                createGoogleUser(account)
                            }
                        } else {
                            createGoogleUser(account)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        onFinishLoading()
                        _errorString.value = databaseError.message
                    }
                })
            } else {
                onFinishLoading()
                _errorString.value = "Не удалось авторизоваться."
            }
        }
    }

    private fun createGoogleUser(account: GoogleSignInAccount) {
        val userId = mAuth.currentUser!!.uid
        val items = HashMap<String, Any>()
        items["userID"] = userId
        items["email"] = account.email!!
        items["fullName"] = account.displayName!!
        items["address"] = ""
        items["profilePictureURL"] = account.photoUrl.toString()

        val userRef = dbReference.child("users").child(userId)
        userRef.setValue(items)
            .addOnSuccessListener {
                val userInfo = UserModel()
                userInfo.userID = userId
                userInfo.email = items["email"].toString()
                userInfo.fullName = items["fullName"].toString()
                userInfo.address = items["address"].toString()
                userInfo.profilePictureURL = items["profilePictureURL"].toString()
                userInfo.active = true
                MyApplication.currentUser = userInfo
                prefs.saveUser(userInfo)

                onFinishLoading()
                startHomeNavigation()
            }
            .addOnFailureListener { e ->
                _errorString.value = e.message
                onFinishLoading()
            }
    }

    fun firebaseAuthWithVK(vkId: UserId, email: String?, fullName: String?, access_token: String?) {
        if (vkId == null) return
        onStartLoading()
        val password = "vkAuth@" + vkId.value.toString() //TODO: Нужен более безопасный способ создания пароля
        mAuth.signInWithEmailAndPassword(email!!, password).addOnCompleteListener {
            if (it.isSuccessful) {
                getFirebaseUserData()
            } else {
                createVKUser(vkId, email, fullName)
            }
        }

    }

    private fun createVKUser(vkId: UserId, email: String?, fullName: String?) {
        val password = "vkAuth@" + vkId.value.toString() //TODO: Нужен более безопасный способ создания пароля
        mAuth.createUserWithEmailAndPassword(email!!, password).addOnSuccessListener { authResult ->
            val userId = authResult.user?.uid

            if (userId != null) {
                val userRef: DatabaseReference = database.reference.child("users").child(userId)
                val userModel = UserModel(userId, email, fullName!!, "", "", true)

                userRef.setValue(userModel).addOnSuccessListener {
                    MyApplication.currentUser = userModel
                    Prefs.getInstance(MyApplication.getInstance()).saveUser(userModel)
                    _navigateToHome.value = true
                }.addOnFailureListener {
                    _errorString.value = "Не удалось авторизоваться [5]."
                }
            }
        }.addOnFailureListener {
            _errorString.value = "Не удалось авторизоваться [4]."
        }
    }

}
