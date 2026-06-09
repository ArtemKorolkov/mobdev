package com.student.mobile_dev_laboratory_work_3.data.model

import com.google.gson.annotations.SerializedName

/** Сообщение с сервера faerytea.name */
data class MessageDto(
    @SerializedName("id") val id: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String?,
    @SerializedName("data") val data: MessageContentDto,
    @SerializedName("time") val time: String?,
)

/** Содержимое сообщения: текст или картинка (взаимоисключающие) */
data class MessageContentDto(
    @SerializedName("Text") val text: TextPayloadDto?,
    @SerializedName("Image") val image: ImagePayloadDto?,
)

data class TextPayloadDto(
    @SerializedName("text") val text: String,
)

data class ImagePayloadDto(
    @SerializedName("link") val link: String,
)

/** Тело запроса POST /messages для отправки текста */
data class OutgoingMessageDto(
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("data") val data: OutgoingContentDto,
)

data class OutgoingContentDto(
    @SerializedName("Text") val text: TextPayloadDto,
)
