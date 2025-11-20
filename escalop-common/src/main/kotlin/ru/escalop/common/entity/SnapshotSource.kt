package ru.escalop.common.entity


import jakarta.persistence.*

/**
 * SnapshotSource
 * snapshot_id = id,
 * year
 * index
 */
@Entity
@Table(name = "snapshot_sources")
class SnapshotSource(


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    open var snapshot: Snapshot,

    @Column(name = "year", nullable = false)
    open var year: Int,

    @Column(name = "index", nullable = false)
    open var index: Int
)