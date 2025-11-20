package ru.escalop.common.remotestorage

interface RemoteStorage {

    suspend fun readData(path: String): String?

    suspend fun writeData(path: String, data: ByteArray): Unit

    companion object {
        val NONE = object : RemoteStorage {
            override suspend fun readData(path: String): String? {
                TODO("Not yet implemented")
            }

            override suspend fun writeData(path: String, data: ByteArray) {
                TODO("Not yet implemented")
            }
        }
    }
}