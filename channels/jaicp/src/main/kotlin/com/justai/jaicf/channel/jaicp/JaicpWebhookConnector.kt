package com.justai.jaicf.channel.jaicp

import com.justai.jaicf.api.BotApi
import com.justai.jaicf.channel.http.HttpBotChannel
import com.justai.jaicf.channel.http.HttpBotRequest
import com.justai.jaicf.channel.http.HttpBotResponse
import com.justai.jaicf.channel.http.asJsonHttpBotResponse
import com.justai.jaicf.channel.jaicp.channels.JaicpNativeBotChannel
import com.justai.jaicf.channel.jaicp.dto.ChannelConfig
import com.justai.jaicf.channel.jaicp.dto.JaicpAsyncResponse
import com.justai.jaicf.channel.jaicp.dto.JaicpBotResponse
import com.justai.jaicf.channel.jaicp.dto.JaicpErrorResponse
import com.justai.jaicf.channel.jaicp.dto.JaicpPingRequest
import com.justai.jaicf.channel.jaicp.endpoints.ktor.channelCheckEndpoint
import com.justai.jaicf.channel.jaicp.endpoints.ktor.healthCheckEndpoint
import com.justai.jaicf.channel.jaicp.endpoints.ktor.reloadConfigEndpoint
import com.justai.jaicf.channel.jaicp.http.HttpClientFactory
import com.justai.jaicf.helpers.logging.WithLogger
import io.ktor.client.*
import io.ktor.client.features.logging.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * This class is used to process [HttpBotRequest] with [JaicpCompatibleBotChannel] channels.
 * Supported channels are [JaicpCompatibleBotChannel], [JaicpNativeBotChannel], [JaicpCompatibleAsyncBotChannel].
 *
 * NOTE:
 * In general cases, you should use [JaicpServer] to establish webhook connection between JAICP and your bot,
 * as it provides required endpoints: [channelCheckEndpoint], [healthCheckEndpoint], [reloadConfigEndpoint].
 *
 * Usage example:
 * ```kotlin
 * embeddedServer(Netty, 8000) {
 *  routing {
 *      httpBotRouting(
 *          "/" to JaicpWebhookConnector(
 *              botApi = telephonyCallScenario,
 *              accessToken = accessToken,
 *              channels = listOf(TelephonyChannel)
 *          ))
 *      }
 *  }.start(wait = true)
 * ```
 *
 * @see JaicpNativeBotChannel
 * @see JaicpCompatibleBotChannel
 * @see JaicpCompatibleAsyncBotChannel
 * @see JaicpServer
 *
 * @param botApi the [BotApi] implementation used to process the requests for all channels
 * @param accessToken can be configured in JAICP Web Interface
 * @param channels is a list of channels which will be managed by connector
 * */

open class JaicpWebhookConnector(
    botApi: BotApi,
    accessToken: String,
    url: String = DEFAULT_PROXY_URL,
    channels: List<JaicpChannelFactory>,
    logLevel: LogLevel = LogLevel.INFO,
    httpClient: HttpClient = HttpClientFactory.create(logLevel),
    executor: Executor = DEFAULT_EXECUTOR
) : WithLogger,
    HttpBotChannel,
    JaicpConnector(botApi, channels, accessToken, url, httpClient, executor) {

    constructor(
        botApi: BotApi,
        accessToken: String,
        url: String = DEFAULT_PROXY_URL,
        channels: List<JaicpChannelFactory>,
        logLevel: LogLevel = LogLevel.INFO,
        httpClient: HttpClient = HttpClientFactory.create(logLevel),
        executorThreadPoolSize: Int
    ) : this(
        botApi,
        accessToken,
        url,
        channels,
        logLevel,
        httpClient,
        Executors.newFixedThreadPool(executorThreadPoolSize)
    )

    protected val channelMap = mutableMapOf<String, JaicpBotChannel>()

    init {
        loadConfig()
    }

    override fun register(channel: JaicpBotChannel, channelConfig: ChannelConfig) {
        if (!channelMap.containsKey(channelConfig.channel)) {
            channelMap[channelConfig.channel] = channel
            logger.debug("Register channel ${channelConfig.channelType}")
        }
    }

    override fun evict(channelConfig: ChannelConfig) {
        logger.debug("Evict channel ${channelConfig.channelType}")
        channelMap.remove(channelConfig.channel)
    }

    override fun getRunningChannels() = channelMap

    fun reload() = reloadConfig()

    override fun process(request: HttpBotRequest): HttpBotResponse {
        val botRequest = request.receiveText()
            .also { logger.debug("Received botRequest: $it") }
            .apply { if (isHandledPingQuery(this)) return "{}".asJsonHttpBotResponse() }
            .asJaicpBotRequest()
            .also { JaicpMDC.setFromRequest(it) }

        val channel = channelMap[botRequest.channelBotId]
            ?: return HttpBotResponse.notFound("Channel ${botRequest.channelType} is not configured or not supported")

        val response = processJaicpRequest(botRequest, channel)
        return when(response){
            is JaicpAsyncResponse -> HttpBotResponse.accepted()
            is JaicpBotResponse -> response.serialized().asJsonHttpBotResponse()
            is JaicpErrorResponse -> HttpBotResponse.error(response.message)
        }
    }

    private fun isHandledPingQuery(request: String): Boolean = try {
        val req = JSON.decodeFromString(JaicpPingRequest.serializer(), request)
        req.requestType == PING_REQUEST_TYPE && channelMap.containsKey(req.botId)
    } catch (e: Exception) {
        false
    }
}
