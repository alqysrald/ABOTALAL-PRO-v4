package com.example.data.mikrotik

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class PingResult(
    val isReachable: Boolean,
    val latencyMs: Long,
    val message: String
)

object PingUtility {
    suspend fun pingHost(host: String, port: Int = 80, timeoutMs: Int = 10000): PingResult = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        val startTime = System.currentTimeMillis()

        // 1. Try Socket connection
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(cleanHost, if (port > 0) port else 80), timeoutMs)
            val latency = System.currentTimeMillis() - startTime
            return@withContext PingResult(
                isReachable = true,
                latencyMs = latency,
                message = "متصل بنجاح (${latency}ms)"
            )
        } catch (_: Exception) {
            // Fallback to InetAddress.isReachable
            try {
                val inet = InetAddress.getByName(cleanHost)
                val reachable = inet.isReachable(timeoutMs)
                val latency = System.currentTimeMillis() - startTime
                if (reachable) {
                    return@withContext PingResult(
                        isReachable = true,
                        latencyMs = latency,
                        message = "متصل بنجاح (${latency}ms)"
                    )
                }
            } catch (e: Exception) {
                return@withContext PingResult(
                    isReachable = false,
                    latencyMs = 0,
                    message = e.localizedMessage ?: "تعذر الوصول للعنوان"
                )
            }
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }

        PingResult(
            isReachable = false,
            latencyMs = 0,
            message = "انتهت المهلة دون استجابة"
        )
    }
}
