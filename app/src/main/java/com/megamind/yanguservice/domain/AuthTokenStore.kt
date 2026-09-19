package com.megamind.yanguservice.domain

import com.megamind.yanguservice.utlis.Result

interface AuthTokenStore {
    fun getToken(): String?
    suspend fun saveToken(token: String): Result<Unit>
    suspend fun clearToken()
}
