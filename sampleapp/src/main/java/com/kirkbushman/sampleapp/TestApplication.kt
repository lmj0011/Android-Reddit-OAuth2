package com.kirkbushman.sampleapp

import android.app.Application
import com.kirkbushman.auth.RedditAuth
import com.kirkbushman.auth.managers.SharedPrefsStorageManager
import com.kirkbushman.auth.models.AuthType
import com.kirkbushman.auth.models.TokenBearer
import com.kirkbushman.auth.utils.Utils.buildRetrofit
import org.xmlpull.v1.XmlPullParser

class TestApplication : Application() {

    companion object {
        lateinit var instance: TestApplication
    }

    var authClient: RedditAuth? = null

    fun loadClient(authType: AuthType? = null) {

        val creds = loadCredsFromFile()

        authClient = if (authType != null) {
            getRedditAuth(creds, authType)
        } else {

            val storManager = SharedPrefsStorageManager(this)
            if (storManager.isAuthed()) {

                getRedditAuth(creds, storManager.authType())
            } else {

                null
            }
        }
    }

    var savedBearer: TokenBearer? = null
    fun getBearer(): TokenBearer? {

        if (savedBearer == null) {

            if (authClient?.hasSavedBearer() == true) {
                savedBearer = authClient?.getSavedBearer()
            }
        }

        return savedBearer
    }

    fun setBearer(bearer: TokenBearer) {
        this.savedBearer = bearer
    }

    init {
        instance = this
    }

    private fun loadCredsFromFile(): TestCredentials {
        val xpp = resources.getXml(R.xml.credentials)

        var clientId = ""
        var redirectUrl = ""

        var scriptClientId = ""
        var scriptClientSecret = ""
        var username = ""
        var password = ""

        val scopes = ArrayList<String>()

        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {

            when (xpp.eventType) {

                XmlPullParser.START_TAG -> {

                    when (xpp.name) {
                        "clientId" -> clientId = xpp.nextText()
                        "redirectUrl" -> redirectUrl = xpp.nextText()
                        "scope" -> scopes.add(xpp.nextText())
                        "scriptClientId" -> scriptClientId = xpp.nextText()
                        "scriptClientSecret" -> scriptClientSecret = xpp.nextText()
                        "username" -> username = xpp.nextText()
                        "password" -> password = xpp.nextText()
                    }
                }
            }

            xpp.next()
        }

        return TestCredentials(
            clientId,
            redirectUrl,

            scriptClientId,
            scriptClientSecret,
            username,
            password,

            scopes
        )
    }

    private fun getRedditAuth(creds: TestCredentials, authType: AuthType?): RedditAuth? {

        return when (authType) {
            AuthType.INSTALLED_APP -> {

                RedditAuth.Builder()
                    .setRetrofit(buildRetrofit("https://github.com", true))
                    .setApplicationCredentials(creds.clientId, creds.redirectUrl)
                    .setScopes(creds.scopes.toTypedArray())
                    .setStorageManager(SharedPrefsStorageManager(this))
                    .setLogging(true)
                    .build()
            }

            AuthType.USERLESS -> {

                RedditAuth.Builder()
                    .setRetrofit(buildRetrofit("https://github.com", true))
                    .setUserlessCredentials(creds.clientId)
                    .setScopes(creds.scopes.toTypedArray())
                    .setStorageManager(SharedPrefsStorageManager(this))
                    .setLogging(true)
                    .build()
            }

            AuthType.SCRIPT -> {

                RedditAuth.Builder()
                    .setRetrofit(buildRetrofit("https://github.com", true))
                    .setScriptAuthCredentials(
                        creds.username,
                        creds.password,
                        creds.scriptClientId,
                        creds.scriptClientSecret
                    )
                    .setScopes(creds.scopes.toTypedArray())
                    .setStorageManager(SharedPrefsStorageManager(this))
                    .setLogging(true)
                    .build()
            }

            else -> null
        }
    }
}
