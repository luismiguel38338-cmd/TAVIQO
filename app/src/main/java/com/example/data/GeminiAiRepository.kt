package com.example.data

import android.util.Log
import com.example.model.AiCreatorSuggestion
import com.example.model.AiModerationResult
import com.example.model.Clip
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepository"

class GeminiAiRepository {

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Eres el asistente técnico y de creadores de TAVIQO (El mundo es tu escenario). " +
                        "Ayudas a los usuarios reales con ideas de guiones, descripciones, hashtags, " +
                        "buenas prácticas de transmisiones en vivo, tienda de regalos y normas de seguridad. " +
                        "REGLA ESTRICTA: Nunca inventes usuarios, seguidores, comentarios, métricas ni interacciones falsas. " +
                        "Responde en español claro, directo y profesional."
                )
            }
        )
    }

    private val moderationModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "safe" to Schema.boolean(description = "true if safe for a PG-13 social video platform"),
                "riskLevel" to Schema.enumeration(listOf("low", "medium", "high"), description = "Safety risk level"),
                "reason" to Schema.string(description = "Short explanation in Spanish"),
                "suggestedAction" to Schema.enumeration(listOf("approve", "review", "reject"), description = "Recommended action")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val creatorSuggestionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "caption" to Schema.string(description = "Engaging short video caption in Spanish under 180 chars"),
                "hashtags" to Schema.array(
                    items = Schema.string(description = "Hashtag starting with # without spaces"),
                    description = "4 to 6 relevant hashtags"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    suspend fun moderateContent(textToEvaluate: String): Result<AiModerationResult> {
        return try {
            val response = withTimeout(45_000L) {
                moderationModel.generateContent(
                    "Evalúa la seguridad PG-13 de este contenido publicado por un usuario en TAVIQO " +
                        "(detecta odio, acoso, violencia explícita, estafas, contenido adulto o spam): \"$textToEvaluate\""
                )
            }
            val raw = response.text ?: throw IllegalStateException("Gemini devolvió una respuesta vacía.")
            val json = JSONObject(raw)
            Result.success(
                AiModerationResult(
                    safe = json.optBoolean("safe", true),
                    riskLevel = json.optString("riskLevel", "low"),
                    reason = json.optString("reason", "Contenido evaluado"),
                    suggestedAction = json.optString("suggestedAction", "approve")
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Moderation call failed", e)
            Result.failure(e)
        }
    }

    suspend fun suggestCaptionAndHashtags(topic: String): Result<AiCreatorSuggestion> {
        return try {
            val response = withTimeout(45_000L) {
                creatorSuggestionModel.generateContent(
                    "Crea una descripción atractiva y hashtags relevantes para un vídeo vertical corto en TAVIQO sobre: \"$topic\""
                )
            }
            val raw = response.text ?: throw IllegalStateException("Gemini devolvió una respuesta vacía.")
            val json = JSONObject(raw)
            val caption = json.optString("caption", "")
            val tagsArray = json.optJSONArray("hashtags")
            val hashtags = buildList {
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        val tag = tagsArray.optString(i).trim()
                        if (tag.isNotBlank()) {
                            add(if (tag.startsWith("#")) tag else "#$tag")
                        }
                    }
                }
            }.take(6)
            Result.success(AiCreatorSuggestion(caption = caption, hashtags = hashtags))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Suggestion call failed", e)
            Result.failure(e)
        }
    }

    fun askAssistantStream(prompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                assistantModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("El asistente no generó texto.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming assistant failed", e)
            throw e
        }
    }

    suspend fun rankRecommendedClips(likedHashtags: List<String>, realClips: List<Clip>): Result<List<String>> {
        if (realClips.size <= 1) return Result.success(realClips.map { it.id })
        return try {
            val catalog = realClips.take(25).joinToString("\n") { clip ->
                "id=${clip.id} | tags=${clip.hashtags.joinToString(",")} | likes=${clip.likesCount} | views=${clip.viewsCount} | caption=${clip.caption.take(60)}"
            }
            val schema = Schema.obj(
                properties = mapOf(
                    "orderedIds" to Schema.array(
                        items = Schema.string(),
                        description = "Exact clip IDs ordered by relevance"
                    )
                )
            )
            val rankingModel = Firebase.ai.generativeModel(
                modelName = "gemini-3.5-flash-lite",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                }
            )
            val response = withTimeout(30_000L) {
                rankingModel.generateContent(
                    "Ordena únicamente estos IDs reales de clips según la actividad del usuario. " +
                        "Hashtags preferidos del usuario: ${likedHashtags.ifEmpty { listOf("general") }}.\nClips reales:\n$catalog"
                )
            }
            val raw = response.text ?: return Result.success(realClips.map { it.id })
            val arr = JSONObject(raw).optJSONArray("orderedIds") ?: return Result.success(realClips.map { it.id })
            val validIds = realClips.map { it.id }.toSet()
            val ranked = (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).takeIf { it in validIds }
            }.distinct()
            val missing = realClips.map { it.id }.filterNot { it in ranked }
            Result.success(ranked + missing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Recommendation ranking fallback to chronological", e)
            Result.failure(e)
        }
    }
}
