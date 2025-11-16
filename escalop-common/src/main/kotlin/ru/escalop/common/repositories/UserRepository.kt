package ru.escalop.ru.escalop.common.repositories

import ru.escalop.ru.escalop.common.entity.UserEntity
import jakarta.persistence.EntityManager

interface UserRepository {

    /**
     * Поиск пользователя по логину (для авторизации).
     */
    fun findByLogin(login: String): UserEntity?
}


class UserRepositoryImpl(
    private val em: EntityManager
) : UserRepository {

    override fun findByLogin(login: String): UserEntity? {
        val query = em.createQuery(
            "select u from UserEntity u where u.login = :login",
            UserEntity::class.java
        )
        query.setParameter("login", login)
        return query.resultList.firstOrNull()
    }
}