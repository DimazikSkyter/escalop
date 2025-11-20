package ru.escalop.common.entity

import jakarta.persistence.*
import ru.escalop.common.model.AnalysisType

@Entity
@Table(name = "analysis_types")
class AnalysisTypeEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    @Column(name = "code", nullable = false, unique = true, length = 100)
    open var code: String,

    @Column(name = "description", length = 255)
    open var description: String? = null
) {
    @OneToMany(mappedBy = "analysisTypeEntity", fetch = FetchType.LAZY)
    open val snapshots: MutableList<Snapshot> = mutableListOf()

    fun toModel(): AnalysisType = AnalysisType.valueOf(code)
}