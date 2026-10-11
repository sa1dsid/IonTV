package com.ionica.iontv.core.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorsTest {

    @Test
    fun `shared auth errors keep their stable code`() {
        val error = httpError(401, """{"code":"REFRESH_INVALID","message":"Invalid refresh token"}""").apiError()

        assertEquals(401, error?.status)
        assertEquals("REFRESH_INVALID", error?.code)
        assertEquals("Invalid refresh token", error?.detail)
    }

    @Test
    fun `native tv errors carry a detail line`() {
        val error = httpError(404, """{"detail":"Pairing code not found"}""").apiError()

        assertEquals(404, error?.status)
        assertNull(error?.code)
        assertEquals("Pairing code not found", error?.detail)
    }

    @Test
    fun `validation errors still give the status`() {
        val body = """{"detail":[{"loc":["body","code"],"msg":"Field required","type":"missing"}]}"""
        val error = httpError(422, body).apiError()

        assertEquals(422, error?.status)
        assertNull(error?.detail)
    }

    @Test
    fun `a dropped connection is not an api error`() {
        assertNull(IOException("connection reset").apiError())
    }

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType())),
    )
}
