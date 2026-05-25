package ru.escalop.common.remotestorage

import ru.escalop.common.model.User

interface RemoteStorage {

    suspend fun readData(user: User, path: String): String?

    suspend fun writeData(user: User, path: String, data: ByteArray): Unit

    companion object {
        val NONE = object : RemoteStorage {
            override suspend fun readData(user: User, path: String): String? {
                TODO("Not yet implemented")
            }

            override suspend fun writeData(user: User, path: String, data: ByteArray) {
                TODO("Not yet implemented")
            }
        }
    }
}