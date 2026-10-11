package com.ionica.iontv.core.api

import javax.inject.Qualifier

/** Клиент без TV-сессии и без обновления токена — им ходит сам TokenAuthenticator. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Unauthenticated
