package com.ionica.iontv.feature.signin.domain.model

enum class LinkFailure {
    /** Пользователь закрыл окно выбора аккаунта: по ТЗ молча возвращаемся на экран. */
    GOOGLE_CANCELLED,
    GOOGLE_UNAVAILABLE,
    EMAIL_INVALID,

    /** Неверный, протухший или сгоревший после пяти попыток код из письма. */
    CODE_INVALID,
    RESEND_TOO_SOON,
    NO_CONNECTION,
    SERVER,
}

class LinkException(val failure: LinkFailure) : Exception(failure.name)
