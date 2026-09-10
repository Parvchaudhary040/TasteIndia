package com.parv.tasteindia.data.remote

import com.parv.tasteindia.domain.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorsTest {

    @Test
    fun `http exception keeps its status code`() {
        val error = HttpException(
            Response.error<Any>(503, "".toResponseBody("text/plain".toMediaType())),
        )
        assertEquals(AppError.Http(503), error.toAppError())
    }

    @Test
    fun `socket and interrupted io timeouts map to Timeout`() {
        assertEquals(AppError.Timeout, SocketTimeoutException().toAppError())
        assertEquals(AppError.Timeout, InterruptedIOException("timeout").toAppError())
    }

    @Test
    fun `dns failure and generic io map to Network`() {
        assertEquals(AppError.Network, UnknownHostException().toAppError())
        assertEquals(AppError.Network, IOException("socket closed").toAppError())
    }

    @Test
    fun `serialization failure maps to Serialization`() {
        assertEquals(AppError.Serialization, SerializationException("bad json").toAppError())
    }

    @Test
    fun `anything else maps to Unknown`() {
        assertEquals(AppError.Unknown, IllegalStateException().toAppError())
    }

    @Test
    fun `cancellation is rethrown, never swallowed`() {
        assertThrows(CancellationException::class.java) {
            CancellationException("stop").toAppError()
        }
    }
}
