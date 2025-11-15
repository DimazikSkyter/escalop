package ru.escalop.ru.escalop.common.repositories

import ru.escalop.ru.escalop.common.entity.User
import jakarta.persistence.EntityManager

interface UserRepository {

    /**
     * Поиск пользователя по логину (для авторизации).
     */
    fun findByLogin(login: String): User?
}


class UserRepositoryImpl(
    private val em: EntityManager
) : UserRepository {

    override fun findByLogin(login: String): User? {
        val query = em.createQuery(
            "select u from User u where u.login = :login",
            User::class.java
        )
        query.setParameter("login", login)
        return query.resultList.firstOrNull()
    }
}