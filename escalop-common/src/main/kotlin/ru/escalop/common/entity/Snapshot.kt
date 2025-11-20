package ru.escalop.common.entity


import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "snapshots")
class Snapshot(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    open var userEntity: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_type_id", nullable = false)
    open var analysisTypeEntity: AnalysisTypeEntity,

    @Column(name = "local_date", nullable = true)
    open var localDate: LocalDate?,

    @Column(name = "document_name", nullable = false, length = 512)
    open var documentName: String,

    @Column(name = "metrics", columnDefinition = "jsonb")
    open var metrics: String
) {
    @OneToOne(mappedBy = "snapshot", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, optional = true)
    open var source: SnapshotSource? = null

    @OneToMany(mappedBy = "snapshot", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    open val statuses: MutableList<SnapshotStatus> = mutableListOf()
}