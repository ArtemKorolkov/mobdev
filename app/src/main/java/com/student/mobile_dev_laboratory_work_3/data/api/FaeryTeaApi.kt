package com.student.mobile_dev_laboratory_work_3.data.api

import com.student.mobile_dev_laboratory_work_3.data.model.LoginRequest
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.OutgoingMessageDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** REST API сервера https://faerytea.name/ */
interface FaeryTeaApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{channelName}")
    suspend fun getChannelMessages(
        @Path("channelName") channelName: String,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
        @Query("lastKnownId") lastKnownId: String = "0",
        @Query("reverse") reverse: Boolean = false,
    ): List<MessageDto>

    @POST("messages")
    suspend fun sendMessage(@Body message: OutgoingMessageDto): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://faerytea.name/"
        const val DEFAULT_LIMIT = 20
    }
}
