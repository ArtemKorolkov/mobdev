package com.student.mobile_dev_laboratory_work_3.data.local.entity

/**
 * Сообщение в локальной БД.
 * id — серверный id или local_* для очереди отправки.
 */
data class MessageEntity(
    val id: String,
    val channelName: String,
    val from: String,
    val to: String?,
    val time: String?,
    val textBody: String?,
    val imageLink: String?,
    val isPending: Boolean,
    val sortKey: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
