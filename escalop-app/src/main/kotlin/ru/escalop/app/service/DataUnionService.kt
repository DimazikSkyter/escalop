package ru.escalop.app.service

import ru.escalop.common.model.AnalysisType
import ru.escalop.common.model.HealthDataResult
import ru.escalop.common.model.StorageHealthPart


class DataUnionService {
    fun union(storageHealthPart: StorageHealthPart, healthDataResult: HealthDataResult): StorageHealthPart {
        if(healthDataResult.analysisType.equals(AnalysisType.valueOf(storageHealthPart.analysisType))) {
            throw RuntimeException("Parts must be the same type, but storage file is ${storageHealthPart.analysisType}" +
                    " and income file is ${healthDataResult.analysisType}")
        }
        if(healthDataResult.date != null && healthDataResult.date!!.year != storageHealthPart.year) {
            throw RuntimeException("Parts must be the same year, but storage file year is ${storageHealthPart.year}" +
                    " and income file is ${healthDataResult.date!!.year}")
        }
        (storageHealthPart.metrics as MutableList).add(healthDataResult)
        return storageHealthPart
    }
}