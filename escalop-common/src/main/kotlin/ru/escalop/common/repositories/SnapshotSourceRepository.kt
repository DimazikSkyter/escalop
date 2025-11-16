package ru.escalop.ru.escalop.common.repositories

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import ru.escalop.ru.escalop.common.entity.AnalysisTypeEntity
import ru.escalop.ru.escalop.common.entity.Snapshot
import ru.escalop.ru.escalop.common.entity.SnapshotSource

interface SnapshotSourceRepository {
    fun save(entity: SnapshotSource): SnapshotSource

    fun getSourceBySnapshot(snapshot: Snapshot): SnapshotSource
}

class SnapshotSourceRepositoryImp (
    private val em: EntityManager
) : SnapshotSourceRepository {
    override fun save(entity: SnapshotSource): SnapshotSource {
        return if (entity.snapshotId == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    override fun getSourceBySnapshot(snapshot: Snapshot): SnapshotSource {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(SnapshotSource::class.java)
        val root = cq.from(SnapshotSource::class.java)

        val predicates = mutableListOf<Predicate>().apply {
            add(cb.equal(root.get<Long>("snapshot_id"), snapshot.id))
        }

        cq.select(root)
            .where(*predicates.toTypedArray())

        return em.createQuery(cq).singleResult
    }

}