package ru.lansonz.dayquestion.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import ru.lansonz.dayquestion.model.UserModel

object RealtimeDatabaseUtil {
    private val databaseInstance: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    private val currentUserRef: DatabaseReference
        get() = databaseInstance.child(
            "users/${FirebaseAuth.getInstance().currentUser?.uid
                ?: throw NullPointerException("UID is null.")}"
        )

    fun updateUser(userModel: UserModel, onComplete: (String) -> Unit) {
        currentUserRef.setValue(userModel)
            .addOnSuccessListener {
                onComplete("success")
            }
            .addOnFailureListener {
                onComplete("failure")
            }
    }
}
