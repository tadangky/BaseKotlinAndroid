package com.pratama.baseandroid.data.datasource.remote.interceptor

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.atomic.AtomicBoolean

//https://medium.com/@shakib.possesed/android-token-authenticator-3e7ef6b20b99
class TokenRefreshAuthenticator : Authenticator {

    private var tokenRefreshInProgress: AtomicBoolean = AtomicBoolean(false)
    private var request: Request? = null

    // Gets called when a request returns 401 (Unauthorized)
    override fun authenticate(route: Route?, response: Response): Request? {
        return runBlocking {
            request = null

            // Checking if a token refresh call is already in progress or not
            // The first request will enter the if block
            // Later requests will enter the else block
            if (!tokenRefreshInProgress.get()) {
                tokenRefreshInProgress.set(true)
                // Refreshing token
                refreshToken()
                request = buildRequest(response.request.newBuilder())
                tokenRefreshInProgress.set(false)
            } else {
                // Waiting for the ongoing request to finish
                // So that we don't refresh our token multiple times
                waitForRefresh(response)
            }

            // return null to stop retrying once responseCount returns 3 or above.
            if (responseCount(response) >= 3) {
                null
            } else request
        }
    }

    // Refresh your token here and save them.
    private suspend fun refreshToken() {
        // Simulating a token refresh request
        delay(200)
        ACCESS_TOKEN = "ABC"
        delay(200)
    }

    // Queuing the requests with delay
    private suspend fun waitForRefresh(response: Response) {
        while (tokenRefreshInProgress.get()) {
            delay(100)
        }
        request = buildRequest(response.request.newBuilder())
    }

    private fun responseCount(response: Response?): Int {
        var result = 1
        while (response?.priorResponse != null && result <= 3) {
            result++
        }
        return result
    }

    // Build a new request with new access token
    private fun buildRequest(requestBuilder: Request.Builder): Request {
        return requestBuilder
            .header(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_VALUE)
            .header(HEADER_AUTHORIZATION, HEADER_AUTHORIZATION_TYPE + ACCESS_TOKEN)
            .build()
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_CONTENT_TYPE_VALUE = "application/json"
        const val HEADER_AUTHORIZATION_TYPE = "Bearer "
        // You should use persistent storage like SharedPrefrences or DataStore to store your tokens
        var ACCESS_TOKEN = "123"
    }
}
