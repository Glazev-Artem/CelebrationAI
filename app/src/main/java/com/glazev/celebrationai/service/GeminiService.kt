package com.glazev.celebrationai.service

import android.util.Log
import com.glazev.celebrationai.data.AppLanguage
import com.glazev.celebrationai.data.AppSettings
import com.glazev.celebrationai.data.Celebration
import com.glazev.celebrationai.data.CelebrationTone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val max_tokens: Int = 1500,
    val temperature: Double = 0.9
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice> = emptyList(),
    val error: OpenRouterError? = null
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

@Serializable
data class OpenRouterError(
    val message: String? = null,
    val code: Int? = null
)

class GeminiService(private val apiKey: String, private val settings: AppSettings) {
    
    private val TAG = "GeminiService"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val AUTO_MODEL = "openrouter/auto"

    private fun getSystemRole(): String {
        val langName = when(settings.selectedLanguage) {
            AppLanguage.RU -> "русский"
            AppLanguage.EN -> "английский (English)"
            AppLanguage.KK -> "казахский (Қазақша)"
            AppLanguage.TT -> "татарский (Татарча)"
            AppLanguage.KA -> "грузинский (ქართული)"
            AppLanguage.UZ -> "узбекский (O'zbekcha)"
            AppLanguage.KY -> "киргизский (Кыргызча)"
            AppLanguage.TG -> "таджикский (Тоҷикӣ)"
            AppLanguage.HY -> "армянский (Հայерեն)"
            AppLanguage.AV -> "аварский (Мааруул)"
        }
        
        return "Ты — эксперт по созданию персональных поздравлений и подбору подарков. " +
                "Твой стиль: грамотный, креативный, живой $langName язык. " +
                "ОБЯЗАТЕЛЬНО пиши ответ на языке: $langName. " +
                "ВАЖНО: Если ты пишешь на русском языке, всегда используй букву 'Ё' в словах, где она должна быть (например, 'всё', 'ещё', 'придёт', 'свёкор'). Никогда не заменяй 'Ё' на 'Е'. " +
                "Если выбран тон 'С чёрным юмором', забудь про политкорректность. Пиши максимально ЖЕСТКО, ЦИНИЧНО и ОСТРОУМНО. " +
                "Это должна быть качественная 'прожарка' (roast). Шути про возраст, " +
                "про бесполезность профессии, про нелепые хобби. Юмор должен быть едким и саркастичным. " +
                "Ответ должен содержать только запрошенный контент."
    }

    suspend fun generateGreeting(celebration: Celebration, isApology: Boolean = false): String? {
        val langName = when(settings.selectedLanguage) {
            AppLanguage.RU -> "русский"
            AppLanguage.EN -> "английский"
            AppLanguage.KK -> "казахский"
            AppLanguage.TT -> "татарский"
            AppLanguage.KA -> "грузинский"
            AppLanguage.UZ -> "узбекский"
            AppLanguage.KY -> "киргизский"
            AppLanguage.TG -> "таджикский"
            AppLanguage.HY -> "армянский"
            AppLanguage.AV -> "аварский"
        }
        
        val type = celebration.getEffectiveTypeDisplay()
        val prof = if (celebration.profession.isNotBlank()) "Профессия: ${celebration.profession}" else ""
        val hobby = if (celebration.hobby.isNotBlank()) "Хобби: ${celebration.hobby}" else ""
        
        val apologyContext = if (isApology) "ВАЖНО: Это поздравление с опозданием. Обыграй это максимально унизительно и смешно." else ""
        
        val prompt = """
            Напиши 3 РАЗНЫХ варианта развернутого поздравления на языке: $langName.
            Имя человека: ${celebration.name}.
            Событие: $type.
            Тон: ${celebration.tone.displayName}.
            $prof
            $hobby
            $apologyContext
            
            ИНСТРУКЦИИ:
            1. Максимально интегрируй профессию и хобби в текст.
            2. Если выбран тон с юмором, пиши дерзко и смешно.
            3. Разделяй варианты поздравлений строкой '---'.
            4. Весь текст должен быть строго на языке: $langName.
            5. Помни про обязательное использование буквы 'Ё'.
        """.trimIndent()
        
        return callOpenRouter(prompt, getSystemRole())
    }

    suspend fun generateGiftIdeas(celebration: Celebration): String? {
        val langName = when(settings.selectedLanguage) {
            AppLanguage.RU -> "русский"
            AppLanguage.EN -> "английский"
            AppLanguage.KK -> "казахский"
            AppLanguage.TT -> "татарский"
            AppLanguage.KA -> "грузинский"
            AppLanguage.UZ -> "узбекский"
            AppLanguage.KY -> "киргизский"
            AppLanguage.TG -> "таджикский"
            AppLanguage.HY -> "армянский"
            AppLanguage.AV -> "аварский"
        }
        
        val prompt = """
            Предложи 5 РЕАЛИСТИЧНЫХ идей подарков на языке: $langName.
            Человек: ${celebration.name}.
            Событие: ${celebration.getEffectiveTypeDisplay()}.
            Профессия: ${celebration.profession}.
            Хобби: ${celebration.hobby}.
            
            ФОРМАТ ОТВЕТА (СТРОГО):
            Для каждой идеи используй формат:
            [N]. Название | Поисковый запрос | Тип | Описание
            
            Где:
            - [N] - номер пункта (1, 2, 3...).
            - Название - полное название идеи.
            - Поисковый запрос - 1-2 главных слова для поиска (например "термокружка", "набор инструментов", "фотосессия").
            - Тип - либо 'товар' (для осязаемых вещей), либо 'услуга' (для курсов, сертификатов, впечатлений).
            - Описание - краткое пояснение пользы.
            
            ПРИМЕР:
            1. Качественная термокружка с гравировкой | термокружка | товар | Чтобы кофе всегда оставался горячим во время работы.
            2. Сертификат на индивидуальную фотосессию | фотосессия | услуга | Для создания красивых воспоминаний и новых аватарок.
            
            ТРЕБОВАНИЯ:
            1. Идеи должны быть выполнимыми.
            2. Весь текст должен быть строго на языке: $langName.
            3. Обязательно используй букву 'Ё' во всём тексте.
        """.trimIndent()
        
        return callOpenRouter(prompt, getSystemRole())
    }

    suspend fun generateFunFacts(dateMillis: Long): String? {
        val langCode = when(settings.selectedLanguage) {
            AppLanguage.RU -> "ru"
            AppLanguage.EN -> "en"
            AppLanguage.KK -> "kk"
            AppLanguage.TT -> "tt"
            AppLanguage.KA -> "ka"
            AppLanguage.UZ -> "uz"
            AppLanguage.KY -> "ky"
            AppLanguage.TG -> "tg"
            AppLanguage.HY -> "hy"
            AppLanguage.AV -> "av"
        }
        val langName = when(settings.selectedLanguage) {
            AppLanguage.RU -> "русский"
            AppLanguage.EN -> "английский"
            AppLanguage.KK -> "казахский"
            AppLanguage.TT -> "татарский"
            AppLanguage.KA -> "грузинский"
            AppLanguage.UZ -> "узбекский"
            AppLanguage.KY -> "киргизский"
            AppLanguage.TG -> "таджикский"
            AppLanguage.HY -> "армянский"
            AppLanguage.AV -> "аварский"
        }
        
        val sdf = SimpleDateFormat("d MMMM", Locale(langCode))
        val dateStr = sdf.format(Date(dateMillis))
        val prompt = "Напиши на языке ($langName) 3 интересных исторических факта про день $dateStr и перечисли 3 известных личностей, родившихся в этот день. Используй букву 'Ё' везде, где это необходимо."
        return callOpenRouter(prompt, getSystemRole())
    }

    private suspend fun callOpenRouter(prompt: String, systemRole: String): String? {
        Log.d(TAG, ">>> ЗАПРОС К AI")
        val result = executeRequest(prompt, systemRole, AUTO_MODEL)
        return result?.trim()
    }

    private suspend fun executeRequest(prompt: String, systemRole: String, modelId: String): String? = suspendCoroutine { continuation ->
        try {
            val requestObj = OpenRouterRequest(
                model = modelId,
                messages = listOf(
                    OpenRouterMessage("system", systemRole),
                    OpenRouterMessage("user", prompt)
                )
            )
            val jsonBody = json.encodeToString(OpenRouterRequest.serializer(), requestObj)
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://celebrationai.glazev.ru") 
                .header("X-Title", "CelebrationAI")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Ошибка сети: ${e.message}")
                    continuation.resume(null)
                }
                override fun onResponse(call: Call, response: Response) {
                    val resString = response.body?.string() ?: ""
                    try {
                        val parsed = json.decodeFromString(OpenRouterResponse.serializer(), resString)
                        continuation.resume(parsed.choices.firstOrNull()?.message?.content)
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка парсинга: ${e.message}")
                        continuation.resume(null)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Общая ошибка: ${e.message}")
            continuation.resume(null)
        }
    }
}
