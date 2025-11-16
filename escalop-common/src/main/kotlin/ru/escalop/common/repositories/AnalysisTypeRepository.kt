package ru.escalop.ru.escalop.common.repositories

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import ru.escalop.ru.escalop.common.entity.AnalysisTypeEntity
import ru.escalop.ru.escalop.common.entity.Snapshot

interface AnalysisTypeRepository {
    fun getByName(name: String): AnalysisTypeEntity?
}

class AnalysisTypeRepositoryImpl(
    private val em: EntityManager
) : AnalysisTypeRepository {
    override fun getByName(name: String): AnalysisTypeEntity? {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(AnalysisTypeEntity::class.java)
        val root = cq.from(AnalysisTypeEntity::class.java)

        val predicates = mutableListOf<Predicate>().apply {
            add(cb.equal(root.get<String>("code"), name))
        }

        cq.select(root)
            .where(*predicates.toTypedArray())

        return em.createQuery(cq).singleResult
    }
}