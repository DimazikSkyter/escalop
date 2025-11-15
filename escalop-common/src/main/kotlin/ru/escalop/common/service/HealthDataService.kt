package ru.escalop.ru.escalop.common.service

import ru.escalop.ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.ru.escalop.common.model.Document
import ru.escalop.ru.escalop.common.model.Filter
import ru.escalop.ru.escalop.common.model.HealthSummary
import ru.escalop.ru.escalop.common.model.User
import ru.escalop.ru.escalop.common.remotestorage.RemoteStorage

interface HealthDataService {

    //todo за год, за файл, конкретный тип, конкретная метрика
    fun getDataByFilter(user: User, filter: Filter): HealthSummary

    fun uploadNewDocument(user: User, document: Document)
}

class HealthDataServiceImpl(
    private val dataUnionService: DataUnionService,
    private val remoteStorage: RemoteStorage,
    private val fileMetricExtractorClient: FileMetricExtractorClient
) : HealthDataService {

    override fun getDataByFilter(user: User, filter: Filter): HealthSummary {
        /*
        * идем в базу снапштов с имеющимися фильтрами
        * получили список
        * */
        return HealthSummary(null, mapOf())
    }

    override fun uploadNewDocument(user: User, document: Document) {
        /*
        По базе проверяем есть ли у пользователя этот снапшот
        Проверяем есть этот год и тип, определяем, нужен новый файл или используем старый
        Готовим данные
        Если старый
            Идем за файлом, загружаем, добавляем данные, сохраняем
       Если новый
            Отправляем запрос на сохранение файла
       Возвращаем ответ
        * */
    }
}