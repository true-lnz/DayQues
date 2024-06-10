package ru.lansonz.dayquestion.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
import ru.lansonz.dayquestion.model.UserModel
import ru.lansonz.dayquestion.ui.activity.question.ViewModelFactory

class MyApplication : MultiDexApplication(), LifeCycleDelegate {

    companion object {
        var currentUser: UserModel? = null
        private lateinit var instance: MyApplication

        fun getInstance(): MyApplication {
            if (!::instance.isInitialized) {
                instance = MyApplication()
            }
            return instance
        }
    }



    override fun onCreate() {
        super.onCreate()
        instance = this

        currentUser = Prefs.getInstance(this).getUser()

        val lifeCycleHandler = AppLifecycleHandler(this)
        registerLifecycleHandler(lifeCycleHandler)
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }

    override fun onAppBackgrounded() {
        val mAuth: FirebaseAuth? = FirebaseAuth.getInstance()

        if (mAuth?.currentUser != null && currentUser != null) {
            currentUser?.let {
                it.active = false
                RealtimeDatabaseUtil.updateUser(it) {
                    // Successfully updated user
                }
            }
        }
        Log.d("Awww", "App in background")
    }

    override fun onAppForegrounded() {
        val mAuth: FirebaseAuth? = FirebaseAuth.getInstance()

        if (mAuth?.currentUser != null && currentUser != null) {
            currentUser?.let {
                it.active = true
                RealtimeDatabaseUtil.updateUser(it) {
                    // Successfully updated user
                }
            }
        }

        Log.d("Yeeey", "App in foreground")
    }

    private fun registerLifecycleHandler(lifeCycleHandler: AppLifecycleHandler) {
        registerActivityLifecycleCallbacks(lifeCycleHandler)
        registerComponentCallbacks(lifeCycleHandler)
    }
}
