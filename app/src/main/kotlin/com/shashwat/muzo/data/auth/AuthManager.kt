package com.shashwat.muzo.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.shashwat.muzo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AuthUserState(
    val isSignedIn: Boolean = false,
    val isGuest: Boolean = true,
    val uid: String? = null,
    val displayName: String = "Guest User",
    val email: String? = null,
    val photoUrl: String? = null,
    val isFirebaseConfigured: Boolean = false,
    val errorMessage: String? = null
)

class AuthManager private constructor(private val context: Context) {
    private var auth: FirebaseAuth? = null
    private var googleSignInClient: GoogleSignInClient? = null

    private val _userState = MutableStateFlow(AuthUserState())
    val userState: StateFlow<AuthUserState> = _userState.asStateFlow()

    init {
        checkFirebaseConfig()
    }

    private fun checkFirebaseConfig() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                auth = FirebaseAuth.getInstance()
                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    _userState.value = AuthUserState(
                        isSignedIn = true,
                        isGuest = false,
                        uid = currentUser.uid,
                        displayName = currentUser.displayName ?: "Music Fan",
                        email = currentUser.email,
                        photoUrl = currentUser.photoUrl?.toString(),
                        isFirebaseConfigured = true
                    )
                } else {
                    _userState.value = _userState.value.copy(isFirebaseConfigured = true)
                }
            } else {
                _userState.value = _userState.value.copy(
                    isFirebaseConfigured = false,
                    isGuest = true,
                    displayName = "Guest User"
                )
            }
        } catch (e: Exception) {
            Log.d("AuthManager", "Firebase not yet initialized: ${e.message}")
            _userState.value = _userState.value.copy(
                isFirebaseConfigured = false,
                isGuest = true
            )
        }
    }

    fun getGoogleSignInIntent(): Intent? {
        val defaultClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        }

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (!defaultClientId.isNullOrEmpty()) {
            gsoBuilder.requestIdToken(defaultClientId)
        }

        val gso = gsoBuilder.build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        return googleSignInClient?.signInIntent
    }

    suspend fun handleSignInResult(intent: Intent?): Boolean {
        if (intent == null) return false
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (auth != null && idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth?.signInWithCredential(credential)?.await()
                val user = authResult?.user

                _userState.value = AuthUserState(
                    isSignedIn = true,
                    isGuest = false,
                    uid = user?.uid,
                    displayName = user?.displayName ?: account.displayName ?: "User",
                    email = user?.email ?: account.email,
                    photoUrl = user?.photoUrl?.toString() ?: account.photoUrl?.toString(),
                    isFirebaseConfigured = true
                )
                return true
            } else if (account != null) {
                // Google Sign In without Firebase credential exchange
                _userState.value = AuthUserState(
                    isSignedIn = true,
                    isGuest = false,
                    uid = account.id,
                    displayName = account.displayName ?: "User",
                    email = account.email,
                    photoUrl = account.photoUrl?.toString(),
                    isFirebaseConfigured = false
                )
                return true
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign in error: ${e.message}", e)
            _userState.value = _userState.value.copy(errorMessage = e.message)
        }
        return false
    }

    fun signOut() {
        auth?.signOut()
        googleSignInClient?.signOut()
        _userState.value = AuthUserState(
            isSignedIn = false,
            isGuest = true,
            displayName = "Guest User",
            isFirebaseConfigured = auth != null
        )
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
