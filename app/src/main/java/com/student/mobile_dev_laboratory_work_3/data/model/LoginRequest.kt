package com.student.mobile_dev_laboratory_work_3.data.model

import com.google.gson.annotations.SerializedName

/** Тело запроса POST /login */
data class LoginRequest(
    @SerializedName("name") val name: String,
    @SerializedName("pwd") val password: String,
)
