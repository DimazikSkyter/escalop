package ru.escalop.common.repositories

import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.*
import ru.escalop.common.entity.*
import ru.escalop.common.model.AnalysisType
import ru.escalop.common.model.Filter
import ru.escalop.common.repositories.SnapshotRepositoryImpl
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotRepositoryImplTest {
    private val em = mockk<EntityManager>()
    private val cb = mockk<CriteriaBuilder>()
    private val cq = mockk<CriteriaQuery<Snapshot>>()
    private val root = mockk<Root<Snapshot>>()
    private val statusJoin = mockk<Join<Snapshot, SnapshotStatus>>()
    private val typeJoin = mockk<Join<Snapshot, AnalysisTypeEntity>>()
    private val sourceJoin = mockk<Join<Snapshot, SnapshotSource>>()
    private val typedQuery = mockk<TypedQuery<Snapshot>>()

    private val repository = SnapshotRepositoryImpl(em)

    @Test
    fun `findByFilterAndUser builds criteria with correct paths`() {
        val user = UserEntity(id = 42L, internalKey = "key", login = "login", password = "pwd")

        every { em.criteriaBuilder } returns cb
        every { cb.createQuery(Snapshot::class.java) } returns cq
        every { cq.from(Snapshot::class.java) } returns root
        every { root.join<Snapshot, SnapshotStatus>("statuses") } returns statusJoin

        // цепочка select-distinct-where
        every { cq.select(root) } returns cq
        every { cq.distinct(true) } returns cq
        every { cq.where(*anyVararg<Predicate>()) } returns cq

        every { em.createQuery(cq) } returns typedQuery
        every { typedQuery.resultList } returns emptyList()

        // пути
        val userPath = mockk<Path<UserEntity>>()
        val userIdPath = mockk<Path<Long>>()
        every { root.get<UserEntity>("userEntity") } returns userPath    // ✔ тест отвалится, если будет "user"
        every { userPath.get<Long>("id") } returns userIdPath

        val statePath = mockk<Path<SnapshotState>>()
        every { statusJoin.get<SnapshotState>("state") } returns statePath

        val documentNamePath = mockk<Path<String>>()
        every { root.get<String>("documentName") } returns documentNamePath

        every { root.join<Snapshot, AnalysisTypeEntity>("analysisTypeEntity") } returns typeJoin
        val codePath = mockk<Path<String>>()
        every { typeJoin.get<String>("code") } returns codePath

        val datePath = mockk<Path<LocalDate>>()
        every { root.get<LocalDate>("localDate") } returns datePath

        every { root.join<Snapshot, SnapshotSource>("source", JoinType.LEFT) } returns sourceJoin
        val yearPath = mockk<Path<Int>>()
        every { sourceJoin.get<Int>("year") } returns yearPath

        // предикаты
        every { cb.equal(any(), any<Any>()) } returns mockk()
        every {
            cb.like(
                any<Expression<String>>(),
                any<String>()
            )
        } returns mockk<Predicate>()

        val filter = Filter(
            fileName = "report",
            analysisType = AnalysisType.BLOOD_GENERAL,
            date = LocalDate.now(),
            year = 2024
        )

        val result = repository.findByFilterAndUser(filter, user)

        assertEquals(emptyList<Snapshot>(), result)
    }

    @Test
    fun `findByYearAndType uses analysisType code and userEntity`() {
        val user = UserEntity(id = 1L, internalKey = "key", login = "login", password = "pwd")

        every { em.criteriaBuilder } returns cb
        every { cb.createQuery(Snapshot::class.java) } returns cq
        every { cq.from(Snapshot::class.java) } returns root
        every { cq.select(root) } returns cq
        every { cq.where(*anyVararg<Predicate>()) } returns cq
        every { em.createQuery(cq) } returns typedQuery
        every { typedQuery.resultList } returns emptyList()

        val userPath = mockk<Path<UserEntity>>()
        val userIdPath = mockk<Path<Long>>()
        every { root.get<UserEntity>("userEntity") } returns userPath
        every { userPath.get<Long>("id") } returns userIdPath

        every { root.join<Snapshot, SnapshotSource>("source") } returns sourceJoin
        val yearPath = mockk<Path<Int>>()
        every { sourceJoin.get<Int>("year") } returns yearPath

        every { root.join<Snapshot, AnalysisTypeEntity>("analysisTypeEntity") } returns typeJoin
        val codePath = mockk<Path<String>>()
        every { typeJoin.get<String>("code") } returns codePath

        every { cb.equal(any(), any<Any>()) } returns mockk()

        val result = repository.findByYearAndType(2024, AnalysisType.BLOOD_GENERAL, user)

        assertEquals(emptyList<Snapshot>(), result)
    }
}