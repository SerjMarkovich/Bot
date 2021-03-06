package com.justai.jaicf.channel.jaicp.execution

import com.justai.jaicf.channel.BotChannel
import com.justai.jaicf.channel.invocationapi.InvocableBotChannel
import com.justai.jaicf.channel.invocationapi.InvocationEventRequest
import com.justai.jaicf.channel.jaicp.JSON
import com.justai.jaicf.channel.jaicp.JaicpCompatibleAsyncBotChannel
import com.justai.jaicf.channel.jaicp.JaicpCompatibleBotChannel
import com.justai.jaicf.channel.jaicp.JaicpMDC
import com.justai.jaicf.channel.jaicp.channels.JaicpNativeBotChannel
import com.justai.jaicf.channel.jaicp.dto.JaicpAsyncResponse
import com.justai.jaicf.channel.jaicp.dto.JaicpBotRequest
import com.justai.jaicf.channel.jaicp.dto.JaicpBotResponse
import com.justai.jaicf.channel.jaicp.dto.JaicpErrorResponse
import com.justai.jaicf.channel.jaicp.dto.JaicpResponse
import com.justai.jaicf.channel.jaicp.dto.fromRequest
import com.justai.jaicf.channel.jaicp.invocationapi.InvocationRequestData
import com.justai.jaicf.context.RequestContext
import com.justai.jaicf.helpers.logging.WithLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

class JaicpRequestExecutor(val executorImpl: Executor) : CoroutineScope, WithLogger {
    override val coroutineContext: CoroutineContext = executorImpl.asCoroutineDispatcher()

    fun executeAsync(request: JaicpBotRequest, channel: BotChannel) = async(coroutineContext + MDCContext()) {
        execute(request, channel)
    }

    fun executeSync(request: JaicpBotRequest, channel: BotChannel) = runBlocking {
        executeAsync(request, channel).await()
    }

    private fun execute(request: JaicpBotRequest, channel: BotChannel): JaicpResponse {
        JaicpMDC.setFromRequest(request)
        logger.debug("Processing request: ${JSON.encodeToString(JaicpBotRequest.serializer(), request)}")
        val start = System.currentTimeMillis()
        val response = when (channel) {
            is JaicpNativeBotChannel -> executeNative(channel, request)
            is JaicpCompatibleBotChannel -> executeCompatible(channel, request)
            is JaicpCompatibleAsyncBotChannel -> executeAsync(channel, request)
            else -> JaicpErrorResponse("Not supported channel type ${channel.javaClass}")
        }
        val time = System.currentTimeMillis() - start
        return response.also {
            logger.debug("Response: ${JSON.encodeToString(JaicpResponse.serializer(), it)}. Processing time: $time ms")
        }
    }

    private fun executeNative(channel: JaicpNativeBotChannel, request: JaicpBotRequest) =
        channel.process(request)

    private fun executeCompatible(channel: JaicpCompatibleBotChannel, request: JaicpBotRequest) =
        channel.processCompatible(request)

    private fun executeAsync(channel: JaicpCompatibleAsyncBotChannel, request: JaicpBotRequest): JaicpResponse {
        val isProcessed = tryProcessAsExternalInvocation(channel, request)
        if (!isProcessed) {
            channel.process(request.asHttpBotRequest())
        }

        return JaicpAsyncResponse
    }
}

private fun JaicpCompatibleBotChannel.processCompatible(
    botRequest: JaicpBotRequest,
): JaicpResponse {
    val startTime = System.currentTimeMillis()
    val request = botRequest.asHttpBotRequest()
    val httpBotResponse = process(request)
    val processingTime = System.currentTimeMillis() - startTime

    if (!httpBotResponse.isSuccess()) {
        return JaicpErrorResponse(httpBotResponse.output.toString())
    }

    val rawJson = JSON.decodeFromString<JsonObject>(httpBotResponse.output.toString())
    val response = addRawReply(rawJson)
    return JaicpBotResponse.fromRequest(botRequest, response, processingTime)
}

private fun addRawReply(rawResponse: JsonElement) = buildJsonObject {
    putJsonArray("replies") {
        add(buildJsonObject {
            put("type", "raw")
            put("body", rawResponse)
        })
    }
}

private fun tryProcessAsExternalInvocation(
    channel: JaicpCompatibleAsyncBotChannel,
    request: JaicpBotRequest,
): Boolean {
    if (channel !is InvocableBotChannel) return false
    if (!request.isExternalInvocationRequest()) return false
    val event = request.event ?: return false
    val data = try {
        JSON.decodeFromString(InvocationRequestData.serializer(), request.raw)
    } catch (e: Exception) {
        return false
    }

    channel.processInvocation(
        request = InvocationEventRequest(data.chatId, event, request.raw),
        requestContext = RequestContext.fromHttp(request.asHttpBotRequest())
    )
    return true
}
