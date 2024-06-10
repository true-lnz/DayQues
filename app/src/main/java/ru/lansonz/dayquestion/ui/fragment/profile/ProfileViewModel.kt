package ru.lansonz.dayquestion.ui.fragment.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val _navigateToEdit = MutableLiveData<Boolean>().apply { value = false }
    val navigateToEdit: LiveData<Boolean>
        get() = _navigateToEdit

    private val _profileUpdateSuccess = MutableLiveData<Boolean>()
    val profileUpdateSuccess: LiveData<Boolean>
        get() = _profileUpdateSuccess

    private val _profileUpdateError = MutableLiveData<String>()
    val profileUpdateError: LiveData<String>
        get() = _profileUpdateError

    fun updateProfile(fullName: String, address: String, email: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val imageUri = user.photoUrl
        val userRef = database.reference.child("users").child(userId)

        if (imageUri != null) {
            uploadProfileImage(userId, imageUri) { imageUrl ->
                val updatedData = mapOf(
                    "fullName" to fullName,
                    "address" to address,
                    "email" to email,
                    "profilePictureURL" to imageUrl
                )
                userRef.updateChildren(updatedData)
                    .addOnSuccessListener {
                        updateLocalUserModel(updatedData)
                        _profileUpdateSuccess.value = true
                    }
                    .addOnFailureListener { e ->
                        _profileUpdateError.value = e.message
                    }
            }
        } else {
            val updatedData = mapOf(
                "fullName" to fullName,
                "address" to address,
                "email" to email
            )
            userRef.updateChildren(updatedData)
                .addOnSuccessListener {
                    updateLocalUserModel(updatedData)
                    _profileUpdateSuccess.value = true
                }
                .addOnFailureListener { e ->
                    _profileUpdateError.value = e.message
                }
        }
    }

    private fun uploadProfileImage(userId: String, imageUri: Uri, onSuccess: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef = storageRef.child("images/$userId.png")
        photoRef.putFile(imageUri)
            .addOnSuccessListener {
                photoRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        _profileUpdateError.value = e.message
                    }
            }
            .addOnFailureListener { e ->
                _profileUpdateError.value = e.message
            }
    }

    private fun updateLocalUserModel(updatedData: Map<String, Any?>) {
        val currentUser = MyApplication.currentUser
        currentUser?.apply {
            fullName = updatedData["fullName"] as? String ?: fullName
            address = updatedData["address"] as? String ?: address
            email = updatedData["email"] as? String ?: email
            profilePictureURL = updatedData["profilePictureURL"] as? String ?: profilePictureURL
        }
        Prefs.getInstance(MyApplication.getInstance()).saveUser(currentUser!!)
    }

    fun updateProfilePicture(imageUri: Uri) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        uploadProfileImage(userId, imageUri) { imageUrl ->
            val userRef = database.reference.child("users").child(userId)
            val updatedData = mapOf(
                "profilePictureURL" to imageUrl
            )
            userRef.updateChildren(updatedData)
                .addOnSuccessListener {
                    updateLocalUserModel(updatedData)
                    _profileUpdateSuccess.value = true
                }
                .addOnFailureListener { e ->
                    _profileUpdateError.value = e.message
                }
        }
    }

    fun deleteProfilePicture() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val userRef = database.reference.child("users").child(userId)
        val updatedData = mapOf(
            "profilePictureURL" to ""
        )
        userRef.updateChildren(updatedData)
            .addOnSuccessListener {
                updateLocalUserModel(updatedData)
                _profileUpdateSuccess.value = true
            }
            .addOnFailureListener { e ->
                _profileUpdateError.value = e.message
            }
    }
}
