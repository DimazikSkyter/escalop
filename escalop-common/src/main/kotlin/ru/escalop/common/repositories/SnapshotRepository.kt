package ru.escalop.ru.escalop.common.repositories

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import ru.escalop.ru.escalop.common.entity.*
import ru.escalop.ru.escalop.common.model.AnalysisType
import ru.escalop.ru.escalop.common.model.Filter

interface SnapshotRepository {

    /**
     * 1) Найти снапшоты, которые:
     *  - удовлетворяют фильтру,
     *  - принадлежат конкретному пользователю,
     *  - имеют состояние SAVED_IN_STORAGE.
     */
    fun findByFilterAndUser(filter: Filter, userEntity: UserEntity): List<Snapshot>

    /**
     * 2) Найти все снапшоты по году и типу анализа.
     * отсортировать по индексу
     */
    fun findByYearAndType(year: Int, analysisTypeEntity: AnalysisType, userEntity: UserEntity): List<Snapshot>

    fun save(snapshot: Snapshot): Snapshot
}

class SnapshotRepositoryImpl(
    private val em: EntityManager
) : SnapshotRepository {

    override fun findByFilterAndUser(filter: Filter, userEntity: UserEntity): List<Snapshot> {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Snapshot::class.java)
        val root = cq.from(Snapshot::class.java)

        // join к статусам (обязателен, нам нужен SAVED_IN_STORAGE)
        val statusJoin = root.join<Snapshot, SnapshotStatus>("statuses")

        val predicates = mutableListOf<Predicate>().apply {

            add(cb.equal(root.get<UserEntity>("user").get<Long>("id"), userEntity.id))

            add(cb.equal(statusJoin.get<SnapshotState>("state"), SnapshotState.SAVED_IN_STORAGE))

            filter.fileName?.let { fileName ->
                add(cb.like(root.get("documentName"), "%$fileName%"))
            }

            filter.analysisType?.let { analysisTypeModel ->
                // join к сущности AnalysisType
                val typeJoin = root.join<Snapshot, AnalysisTypeEntity>("analysisType")
                // если в enum нет поля code, используем name/toString()
                add(
                    cb.equal(
                        typeJoin.get<String>("code"),
                        analysisTypeModel.name  // или analysisTypeModel.code, если такое поле есть
                    )
                )
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

    override fun findByYearAndType(year: Int, analysisType: AnalysisType, userEntity: UserEntity): List<Snapshot> {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Snapshot::class.java)
        val root = cq.from(Snapshot::class.java)

        val sourceJoin = root.join<Snapshot, SnapshotSource>("source")
        val typeJoin = root.join<Snapshot, AnalysisTypeEntity>("analysisType")

        val predicates = listOf(
            cb.equal(root.get<UserEntity>("user").get<Long>("id"), userEntity.id),
            cb.equal(sourceJoin.get<Int>("year"), year),
            cb.equal(
                typeJoin.get<String>("code"),
                analysisType.name
            )
        )

        cq.select(root)
            .where(*predicates.toTypedArray())

        return em.createQuery(cq).resultList
    }

    override fun save(snapshot: Snapshot): Snapshot {
        return if (snapshot.id == null) {
            em.persist(snapshot)
            snapshot
        } else {
            em.merge(snapshot)
        }
    }
}