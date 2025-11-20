package ru.escalop.common.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,

    @Column(name = "internal_key", nullable = false, unique = true, length = 255)
    open var internalKey: String,

    @Column(name = "login", nullable = false, unique = true, length = 255)
    open var login: String,

    @Column(name = "password", nullable = false, length = 255)
    open var password: String
) {
    @OneToMany(mappedBy = "userEntity", fetch = FetchType.LAZY)
    open val snapshots: MutableList<Snapshot> = mutableListOf()
}