package com.megamind.yanguservice.utlis


sealed class Result<out T>(val data: T? = null, val e: Throwable? = null) {
    class Success<T>(data: T?) : Result<T>(data)
    class Error<T>(throwable: Throwable) : Result<T>(e = throwable)

}