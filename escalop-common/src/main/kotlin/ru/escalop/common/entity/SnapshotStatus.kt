package ru.escalop.ru.escalop.common.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

enum class SnapshotState {
    REGISTERED,
    PARSED,
    SAVED_IN_STORAGE,
    LOST
}

/**
 * SnapshotStatus
 * id
 * snapshot_id
 * timestamp
 * state can be Registered, Parsed, SaveInStorage
 */
@Entity
@Table(name = "snapshot_statuses")
class SnapshotStatus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    open var snapshot: Snapshot,

    @Column(name = "timestamp", nullable = false)
    open var timestamp: OffsetDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 64)
    open var state: SnapshotState
)