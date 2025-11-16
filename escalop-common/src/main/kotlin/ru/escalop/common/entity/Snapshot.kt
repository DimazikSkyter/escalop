package ru.escalop.ru.escalop.common.entity


import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "snapshots")
class Snapshot(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    /**
     * type_id from AnalysisType
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    open var analysisTypeEntity: AnalysisTypeEntity,

    @Column(name = "local_date", nullable = true)
    open var localDate: LocalDate?,

    @Column(name = "document_name", nullable = false, length = 512)
    open var documentName: String,

    /**
     * user_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    open var userEntity: UserEntity,

    /**
     * metrics as json with can search by xpath
     * columnDefinition можно подстроить под конкретную БД (json/jsonb/text и т.п.)
     */
    @Lob
    @Column(name = "metrics", columnDefinition = "jsonb")
    open var metrics: String
) {
    @OneToOne(mappedBy = "snapshot", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, optional = true)
    open var source: SnapshotSource? = null

    @OneToMany(mappedBy = "snapshot", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    open val statuses: MutableList<SnapshotStatus> = mutableListOf()
}