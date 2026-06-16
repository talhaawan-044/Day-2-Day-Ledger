package com.example.awancoalledger.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.userProfileChangeRequest

class FirebaseManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        auth.addAuthStateListener {
            _currentUser.value = it.currentUser
        }
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1043284371230-l75a44g8vsqo8svpl5viq0r8t1raphkn.apps.googleusercontent.com")
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        auth.signOut()
        getGoogleSignInClient(context).signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun getUserId(): String? = auth.currentUser?.uid

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name
                }
                user.updateProfile(profileUpdates).await()
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Result<Pair<FirebaseUser?, Boolean>> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            Result.success(Pair(result.user, result.additionalUserInfo?.isNewUser == true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkWithCredential(credential: com.google.firebase.auth.AuthCredential): Result<FirebaseUser?> {
        val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        return try {
            val result = user.linkWithCredential(credential).await()
            Result.success(result.user)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous == true

    suspend fun uploadFile(uri: Uri, path: String): String? {
        val userId = getUserId() ?: return null
        return try {
            val ref = storage.reference.child("users/$userId/$path")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFile(path: String) {
        val userId = getUserId() ?: return
        try {
            storage.reference.child("users/$userId/$path").delete().await()
        } catch (e: Exception) {
            // Ignore failure (file might not exist)
        }
    }
}
