package ru.escalop.ru.escalop.common.entity


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

    /**
     * Делаем snapshot_id первичным ключом и одновременно внешним ключом на Snapshot
     */
    @Id
    @Column(name = "snapshot_id")
    open var snapshotId: Long? = null,

    @OneToOne
    @MapsId
    @JoinColumn(name = "snapshot_id")
    open var snapshot: Snapshot,

    @Column(name = "year", nullable = false)
    open var year: Int,

    @Column(name = "idx", nullable = false)
    open var index: Int
)