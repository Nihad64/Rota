package com.nihad64.rotam.data.account

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class Account(val uid: String, val name: String?, val email: String?)
/** Authentication never imports, clears, or reassigns local plans implicitly. */
class AccountService(private val context: Context) {
    val configured = FirebaseApp.getApps(context).isNotEmpty()
    init {
        if (configured) FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
    private val auth = if (configured) FirebaseAuth.getInstance() else null
    private val _account = MutableStateFlow(auth?.currentUser?.let { Account(it.uid, it.displayName, it.email) })
    val account = _account.asStateFlow()
    init {
        auth?.addAuthStateListener { a -> _account.value = a.currentUser?.let { Account(it.uid, it.displayName, it.email) } }
    }
    suspend fun signIn(activity: Activity) {
        check(configured) { "Hesap hizmeti bu derlemede henüz yapılandırılmadı. Yerel planların korunuyor." }
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        check(id != 0) { "Google giriş yapılandırması eksik." }
        val option = GetSignInWithGoogleOption.Builder(context.getString(id)).build()
        val result = CredentialManager.create(activity).getCredential(activity,
            GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
        check(result is CustomCredential && result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Google hesabı doğrulanamadı." }
        val token = GoogleIdTokenCredential.createFrom(result.data).idToken
        auth!!.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }
    suspend fun signOut() {
        auth?.signOut()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
