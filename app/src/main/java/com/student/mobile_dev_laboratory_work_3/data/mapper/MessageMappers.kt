package com.student.mobile_dev_laboratory_work_3.data.mapper

import com.student.mobile_dev_laboratory_work_3.data.local.entity.MessageEntity
import com.student.mobile_dev_laboratory_work_3.data.model.ChatMessage
import com.student.mobile_dev_laboratory_work_3.data.model.ImagePayloadDto
import com.student.mobile_dev_laboratory_work_3.data.model.MessageContentDto
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.TextPayloadDto

fun MessageDto.toEntity(channelName: String, isPending: Boolean = false): MessageEntity {
    val text = data.text?.text
    val imageLink = data.image?.link
    return MessageEntity(
        id = id,
        channelName = channelName,
        from = from,
        to = to,
        time = time,
        textBody = text,
        imageLink = imageLink,
        isPending = isPending,
        sortKey = id.toLongOrNull() ?: System.currentTimeMillis(),
    )
}

fun MessageEntity.toDto(): MessageDto {
    val content = when {
        imageLink != null -> MessageContentDto(
            text = null,
            image = ImagePayloadDto(imageLink),
        )
        else -> MessageContentDto(
            text = TextPayloadDto(textBody.orEmpty()),
            image = null,
        )
    }
    return MessageDto(
        id = id,
        from = from,
        to = to,
        data = content,
        time = time,
    )
}

fun MessageEntity.toChatMessage(): ChatMessage =
    ChatMessage(
        dto = toDto(),
        isPending = isPending,
    )

fun List<MessageDto>.toEntities(channelName: String): List<MessageEntity> =
    map { it.toEntity(channelName) }
