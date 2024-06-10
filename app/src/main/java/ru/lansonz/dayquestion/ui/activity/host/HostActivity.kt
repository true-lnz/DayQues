package ru.lansonz.dayquestion.ui.activity.host

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import ru.lansonz.dayquestion.model.UserModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.vk.api.sdk.VK
import ru.lansonz.dayquestion.utils.MyApplication
import ru.lansonz.dayquestion.utils.Prefs
import ru.lansonz.dayquestion.utils.RealtimeDatabaseUtil.updateUser
import ru.lansonz.dayquestion.R
import ru.lansonz.dayquestion.databinding.ActivityHostBinding

class HostActivity : AppCompatActivity() {

    private val viewModel: HostViewModel by viewModels()
    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var navController: NavController
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivityHostBinding
    private lateinit var graphCopy: NavGraph
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        binding = DataBindingUtil.setContentView(this, R.layout.activity_host)
        binding.lifecycleOwner = this
        binding.hostViewModel = viewModel
        supportActionBar?.hide()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController

        graphCopy = navController.graph

        // Черный список по отображению toolBar и navBar для фрагментов и активностей
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.homeFragment) {
                navController.graph = graphCopy
            }
            if (destination.id == R.id.onBoarding ||
                destination.id == R.id.authFragment ||      // не отображаем в них
                destination.id == R.id.loginFragment ||
                destination.id == R.id.signUpFragment ||
                destination.id == R.id.signUpAvatar ||
                destination.id == R.id.questionActivity
            ) {
                viewModel.hideNavBar() // выкл navigationBar
                // TODO Вырезать viewModel если не прибавится функционала

            } else {
                viewModel.showNavBar() // включили navigationBar
            }
            if (destination.id == R.id.profileFragment) {
                if (MyApplication.currentUser?.fullName == "") {
                    val snackBar = Snackbar.make(binding.root, "Вы не авторизованы.", Snackbar.LENGTH_LONG)
                    snackBar.show()
                }
            }
            if (destination.id == R.id.addQuesFragment) {
                if (MyApplication.currentUser?.fullName == "") {
                    val snackBar = Snackbar.make(binding.root, "Доступно только авторизованным пользователям.", Snackbar.LENGTH_LONG)
                    snackBar.show()
                    navController.popBackStack()
                }
            }
            if (destination.id == R.id.favoritesFragment) {
                val snackBar = Snackbar.make(binding.root, "Этот функционал еще не реализован.", Snackbar.LENGTH_LONG)
                snackBar.show()
                navController.popBackStack()
            }
        }

        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.main_graph)

        if (!Prefs.getInstance(this).hasCompletedWalkthrough) {
            if (mAuth.currentUser == null) {
                graph.setStartDestination(R.id.authFragment)
            } else {
                //getUserData()
                graph.setStartDestination(R.id.homeFragment)
            }
        } else {
            graph.setStartDestination(R.id.onBoarding)
        }

        navController.graph = graph

        binding.bottomNavigationView.setupWithNavController(navController)

        //getUserData()
    }



    private fun getUserData() {
        val userId = mAuth.currentUser!!.uid
        val userRef = database.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userInfo = dataSnapshot.getValue(UserModel::class.java)
                if (userInfo != null) {
                    MyApplication.currentUser = userInfo
                    MyApplication.currentUser?.let {
                        it.active = true

                        updateUser(it) {
                            // Successfully updated user
                        }
                    }
                } else {
                    navController.navigate(R.id.loginFragment)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                navController.navigate(R.id.loginFragment)
            }
        })
    }

    fun logout() {
        MyApplication.currentUser!!.active = false
        updateUser(MyApplication.currentUser!!) {
            mAuth.signOut()
        }
        googleSignInClient.signOut()
        VK.logout()
        MyApplication.currentUser = null
        navController.navigate(R.id.action_logout)
    }
}