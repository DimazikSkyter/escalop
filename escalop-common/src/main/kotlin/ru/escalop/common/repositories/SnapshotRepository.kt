package ru.escalop.ru.escalop.common.repositories

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import ru.escalop.ru.escalop.common.entity.*
import ru.escalop.ru.escalop.common.model.Filter

interface SnapshotRepository {

    /**
     * 1) Найти снапшоты, которые:
     *  - удовлетворяют фильтру,
     *  - принадлежат конкретному пользователю,
     *  - имеют состояние SAVED_IN_STORAGE.
     */
    fun findByFilterAndUser(filter: Filter, userId: Long): List<Snapshot>

    /**
     * 2) Найти все снапшоты по году и типу анализа.
     */
    fun findByYearAndType(year: Int, analysisType: AnalysisType): List<Snapshot>
}

class SnapshotRepositoryImpl(
    private val em: EntityManager
) : SnapshotRepository {

    override fun findByFilterAndUser(filter: Filter, userId: Long): List<Snapshot> {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Snapshot::class.java)
        val root = cq.from(Snapshot::class.java)

        // join к статусам (обязателен, нам нужен SAVED_IN_STORAGE)
        val statusJoin = root.join<Snapshot, SnapshotStatus>("statuses")

        val predicates = mutableListOf<Predicate>().apply {

            add(cb.equal(root.get<User>("user").get<Long>("id"), userId))

            add(cb.equal(statusJoin.get<SnapshotState>("state"), SnapshotState.SAVED_IN_STORAGE))

            filter.fileName?.let { fileName ->
                add(cb.like(root.get("documentName"), "%$fileName%"))
            }

            filter.analysisType?.let { analysisType ->
                add(cb.equal(root.get<AnalysisType>("analysisType"), analysisType))
            }

            filter.date?.let { date ->
                add(cb.equal(root.get<java.time.LocalDate>("localDate"), date))
            }

            filter.year?.let { year ->
                val sourceJoin = root.join<Snapshot, SnapshotSource>("source", JoinType.LEFT)
                add(cb.equal(sourceJoin.get<Int>("year"), year))
            }
        }

        cq.select(root)
            .distinct(true) // из-за join c статусами
            .where(*predicates.toTypedArray())

        return em.createQuery(cq).resultList
    }

    override fun findByYearAndType(year: Int, analysisType: AnalysisType): List<Snapshot> {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Snapshot::class.java)
        val root = cq.from(Snapshot::class.java)

        val sourceJoin = root.join<Snapshot, SnapshotSource>("source")

        val predicates = listOf(
            cb.equal(sourceJoin.get<Int>("year"), year),
            cb.equal(root.get<AnalysisType>("analysisType"), analysisType)
        )

        cq.select(root)
            .where(*predicates.toTypedArray())

        return em.createQuery(cq).resultList
    }
}