package ru.escalop.ru.escalop.common.entity

import jakarta.persistence.*
import ru.escalop.ru.escalop.common.model.AnalysisType

@Entity
@Table(name = "analysis_types")
class AnalysisTypeEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    /**
     * Тип анализа — можешь потом заменить на enum / отдельное поле, если нужно.
     */
    @Column(name = "code", nullable = false, unique = true, length = 128)
    open var code: String,

    @Column(name = "description")
    open var description: String? = null
) {
    // Обратная связь к снапшотам (опционально, но полезно)
    @OneToMany(mappedBy = "analysisTypeEntity", fetch = FetchType.LAZY)
    open val snapshots: MutableList<Snapshot> = mutableListOf()

    fun toModel(): AnalysisType {
        return AnalysisType.valueOf(code)
    }
}