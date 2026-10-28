package com.ionica.iontv.core.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException

const val BAD_REQUEST = 400

const val UNAUTHORIZED = 401

const val FORBIDDEN = 403

const val NOT_FOUND = 404

const val CONFLICT = 409

const val GONE = 410

const val UNPROCESSABLE = 422

const val TOO_MANY_REQUESTS = 429

const val SERVICE_UNAVAILABLE = 503

private val errorJson = Json { ignoreUnknownKeys = true }

class ApiError(
    val status: Int,
    val code: String?,
    val detail: String?,
)

fun Throwable.apiError(): ApiError? {
    val http = this as? HttpException ?: return null
    val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
    val json = body?.let { runCatching { errorJson.parseToJsonElement(it).jsonObject }.getOrNull() }

    return ApiError(
        status = http.code(),
        code = json?.text("code"),
        detail = json?.text("detail") ?: json?.text("message"),
    )
}

private fun JsonObject.text(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
