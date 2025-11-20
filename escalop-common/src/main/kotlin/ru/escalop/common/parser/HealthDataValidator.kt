package ru.escalop.common.parser

import org.slf4j.LoggerFactory
import ru.escalop.common.model.HealthDataResult
import java.util.function.Function

interface HealthDataValidator {
    fun validate(result: HealthDataResult)
}


class HealthDataWarnValidatorImpl (
    val validationStrategy: Map<String, Function<HealthDataResult, Boolean>>
): HealthDataValidator {

    private val logger = LoggerFactory.getLogger(HealthDataWarnValidatorImpl::class.java)!!

    override fun validate(result: HealthDataResult) {
        validationStrategy.map {
            val checkResult: Boolean = it.value.apply(result)
            Pair(it.key, checkResult)
        }.filter { !it.second }.forEach {
            logger.warn("Failed result validation check ${it.first}")
        }
    }
}
