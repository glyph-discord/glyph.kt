/*
 * ServerDirector.kt
 *
 * Glyph, a Discord bot that uses natural language instead of commands
 * powered by DialogFlow and Kotlin
 *
 * Copyright (C) 2017-2021 by Ian Moore
 *
 * This file is part of Glyph.
 *
 * Glyph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.yttr.glyph.bot.presentation

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import org.json.JSONObject
import org.yttr.glyph.bot.Director
import org.yttr.glyph.bot.directors.messaging.SimpleDescriptionBuilder
import org.yttr.glyph.bot.extensions.log
import java.awt.Color
import java.time.Instant

/**
 * Manages server related events
 */
class ServerDirector(private val configure: Config.(botUserId: String) -> Unit = {}) : Director() {
    /**
     * HOCON-like config for the messaging director
     */
    class Config {
        /**
         * All bot lists to submit stats to
         */
        val botLists: MutableSet<BotList> = mutableSetOf()

        /**
         * Add a server list to publish stats to
         */
        fun botList(vararg botList: BotList): Boolean = botLists.addAll(botList)
    }

    private val client = HttpClient()

    private lateinit var config: Config
    private val botLists by lazy {
        config.botLists
    }

    /**
     * When the client becomes ready
     */
    override fun onReady(event: ReadyEvent) {
        config = Config().apply { configure(event.jda.selfUser.id) }
        updateServerCount(event.jda)
    }

    /**
     * When the client joins a guild
     */
    override fun onGuildJoin(event: GuildJoinEvent) {
        updateServerCount(event.jda)
        event.jda.selfUser.log(event.guild.descriptionEmbed.setTitle("Guild Joined").setColor(Color.GREEN).build())
        log.info("Joined ${event.guild}")
    }

    /**
     * When the client leaves a guild
     */
    override fun onGuildLeave(event: GuildLeaveEvent) {
        updateServerCount(event.jda)
        event.jda.selfUser.log(event.guild.descriptionEmbed.setTitle("Guild Left").setColor(Color.RED).build())
        log.info("Left ${event.guild}")
    }

    /**
     * Updates the server count on the bot list websites
     */
    private fun updateServerCount(jda: JDA) = launch {
        val count = jda.guilds.count()
        val countJSON = JSONObject()
            .put("server_count", count)
            .put("shard_id", jda.shardInfo.shardId)
            .put("shard_count", jda.shardInfo.shardTotal)

        botLists.forEach {
            sendServerCount(it, countJSON)
        }
    }

    private suspend fun sendServerCount(botList: BotList, countJSON: JSONObject) {
        try {
            client.post<String>(botList.apiEndpoint) {
                header("Authorization", botList.token)
                header("Content-Type", "application/json")

                body = countJSON.toString()
            }

            log.debug("Updated ${botList.name} server count")
        } catch (e: ResponseException) {
            log.warn("Failed to update ${botList.name} server count due to ${e.response.status} error!")
        }
    }

    private val Guild.descriptionEmbed: EmbedBuilder
        get() {
            val description = SimpleDescriptionBuilder()
                .addField("Name", this.name)
                .addField("ID", this.id)
                .addField("Members", this.memberCount)
                .build()
            return EmbedBuilder()
                .setDescription(description)
                .setThumbnail(this.iconUrl)
                .setFooter("Logging", null)
                .setTimestamp(Instant.now())
        }
}
