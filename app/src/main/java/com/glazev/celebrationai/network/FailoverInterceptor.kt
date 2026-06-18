package com.glazev.celebrationai.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class FailoverInterceptor : Interceptor {
    private val ruHost = "ru.lavka-app.space"
    private val globalHost = "global.lavka-app.space"

    companion object {
        // Память сессии: если один раз поняли, что мы под VPN, больше не ждем таймаутов
        @Volatile
        var isVpnActive = false
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        if (!originalUrl.host.contains("lavka-app.space")) {
            return chain.proceed(originalRequest)
        }

        // Если уже знаем, что включен VPN — летим сразу через Cloudflare без задержек
        if (isVpnActive) {
            val globalUrl = originalUrl.newBuilder().host(globalHost).build()
            val globalRequest = originalRequest.newBuilder().url(globalUrl).build()
            // Ставим комфортный таймаут для ожидания ответа от ИИ
            return chain.withConnectTimeout(15, TimeUnit.SECONDS)
                        .withReadTimeout(60, TimeUnit.SECONDS)
                        .proceed(globalRequest)
        }

        // Если не знаем статус, пробуем быстрый стук в RU-шлюз (даем всего 3 секунды)
        val ruUrl = originalUrl.newBuilder().host(ruHost).build()
        val ruRequest = originalRequest.newBuilder().url(ruUrl).build()

        try {
            return chain.withConnectTimeout(3, TimeUnit.SECONDS)
                        .withReadTimeout(3, TimeUnit.SECONDS)
                        .proceed(ruRequest)
        } catch (e: IOException) {
            println("[Network] RU шлюз недоступен (похоже на VPN). Включаем резервный канал.")
            
            // Запоминаем, что мы под VPN, чтобы следующие генерации летали мгновенно
            isVpnActive = true 
            
            val globalUrl = originalUrl.newBuilder().host(globalHost).build()
            val globalRequest = originalRequest.newBuilder().url(globalUrl).build()
            
            // Выполняем боевой запрос к Cloudflare с нормальными таймаутами для нейросети
            return chain.withConnectTimeout(15, TimeUnit.SECONDS)
                        .withReadTimeout(60, TimeUnit.SECONDS)
                        .proceed(globalRequest)
        }
    }
}
