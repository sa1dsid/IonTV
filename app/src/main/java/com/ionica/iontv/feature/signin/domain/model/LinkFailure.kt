package com.ionica.iontv.feature.signin.domain.model

enum class LinkFailure {
    /** Пользователь закрыл окно выбора аккаунта: по ТЗ молча возвращаемся на экран. */
    GOOGLE_CANCELLED,
    GOOGLE_UNAVAILABLE,
    NO_CONNECTION,
    SERVER,
}

class LinkException(val failure: LinkFailure) : Exception(failure.name)
