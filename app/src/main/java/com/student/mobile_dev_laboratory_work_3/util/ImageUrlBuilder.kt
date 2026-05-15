package com.student.mobile_dev_laboratory_work_3.util

import com.student.mobile_dev_laboratory_work_3.data.api.FaeryTeaApi

/** Формирует URL для превью и полноразмерных картинок */
object ImageUrlBuilder {

    fun thumbUrl(link: String): String =
        "${FaeryTeaApi.BASE_URL}thumb/$link"

    fun fullUrl(link: String): String =
        "${FaeryTeaApi.BASE_URL}img/$link"
}
