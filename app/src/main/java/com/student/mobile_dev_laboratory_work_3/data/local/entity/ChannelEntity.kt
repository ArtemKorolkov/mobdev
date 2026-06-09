package com.student.mobile_dev_laboratory_work_3.data.local.entity

/** Кэшированный канал (чат) */
data class ChannelEntity(
    val name: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
