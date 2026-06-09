package com.student.mobile_dev_laboratory_work_3.data.api

import java.io.IOException

/** HTTP-ошибка от сервера (аналог retrofit2.HttpException для репозитория) */
class FaeryTeaHttpException(val code: Int, message: String? = null) : IOException(message)
