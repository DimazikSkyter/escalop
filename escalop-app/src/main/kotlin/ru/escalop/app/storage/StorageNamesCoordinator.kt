package ru.escalop.app.storage

import ru.escalop.ru.escalop.common.model.AnalysisType

class StorageNamesCoordinator {

    fun generateStorageName(analysisType: AnalysisType, year: Int, index: Int): String {
        return "data_${analysisType.name}_${year}_$index.json"
    }
}