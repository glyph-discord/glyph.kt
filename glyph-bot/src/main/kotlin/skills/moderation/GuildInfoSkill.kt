/*
 * GuildInfoSkill.kt
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

package org.yttr.glyph.bot.skills.moderation

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.ocpsoft.prettytime.PrettyTime
import org.yttr.glyph.bot.ai.AIResponse
import org.yttr.glyph.bot.directors.messaging.SimpleDescriptionBuilder
import org.yttr.glyph.bot.extensions.asPlainMention
import org.yttr.glyph.bot.extensions.toDate
import org.yttr.glyph.bot.messaging.Response
import org.yttr.glyph.bot.skills.Skill
import java.awt.Color
import java.time.Instant

/**
 * A skill that allows users to ask for different info about a guild
 */
class GuildInfoSkill : Skill("skill.moderation.guildInfo", guildOnly = true) {
    override suspend fun onTrigger(event: MessageReceivedEvent, ai: AIResponse): Response {
        val property: String? = ai.result.getStringParameter("guildProperty")

        return if (property != null) {
            val guild = event.guild
            val content = when (property) {
                "name" -> "This guild is **${guild.name}**."
                "id" -> "The id for ${guild.name} is **${guild.id}**."
                "region" -> "${guild.name} is located in **${guild.regionRaw}**."
                "created" -> {
                    val prettyCreated = PrettyTime().format(guild.timeCreated.toDate())
                    "${guild.name} was created **$prettyCreated** (${guild.timeCreated})."
                }
                "owner" ->
                    "**${guild.retrieveOwner().await().asPlainMention}** is the owner of ${guild.name}."
                "members" -> "${guild.name} has **${guild.members.count()}** members."
                "membersHumans" -> "${guild.name} has **${guild.members.count { !it.user.isBot }}** humans."
                "membersBots" -> "${guild.name} has **${guild.members.count { it.user.isBot }}** bots."
                "channels" -> "${guild.name} has **${guild.textChannels.size + guild.voiceChannels.size}** channels."
                "channelsText" -> "${guild.name} has **${guild.textChannels.size}** text channels."
                "channelsVoice" -> "${guild.name} has **${guild.voiceChannels.size}** voice channels."
                "roles" -> "${guild.name} has **${guild.roles.size}** roles."
                "farm" -> "Servers are no longer checked for bot farming."
                else -> "I'm not sure what property `$property` is for a guild."
            }
            Response.Volatile(content)
        } else {
            Response.Volatile(
                event.guild.getInfoEmbed(
                    "Guild Info",
                    "Moderation",
                    null,
                    true
                )
            )
        }
    }

    /**
     * Get an informational embed about a server
     *
     * @param title  the title of the embed
     * @param footer any footer text to include in the embed
     * @param color  the color of the embed
     * @param showExactCreationDate whether or not to show the exact timestamp for the server creation time
     *
     * @return an embed with the requested server info
     */
    fun Guild.getInfoEmbed(
        title: String?,
        footer: String?,
        color: Color?,
        showExactCreationDate: Boolean = false
    ): MessageEmbed {
        val createdAgo = PrettyTime().format(this.timeCreated.toDate())
        val overviewDescription = SimpleDescriptionBuilder()
            .addField("Name", this.name)
            .addField("ID", this.id)
            .addField("Region", this.regionRaw)
            .addField("Created", "$createdAgo ${if (showExactCreationDate) "(${this.timeCreated})" else ""}")
            .addField("Owner", this.owner?.asMention ?: "?")
            .build()
        val membersDescription = SimpleDescriptionBuilder()
            .addField("Humans", this.members.count { !it.user.isBot })
            .addField("Bots", this.members.count { it.user.isBot })
            .addField("Online", this.members.count { it.onlineStatus == OnlineStatus.ONLINE })
            .addField("Total", this.members.count())
            .build()
        val channelsDescription = SimpleDescriptionBuilder()
            .addField("Text", this.textChannels.count())
            .addField("Voice", this.voiceChannels.count())
            .addField("Categories", this.categories.count())
            .build()
        val rolesDescription = SimpleDescriptionBuilder()
            .addField("Total", this.roles.count())
            .addField("List", this.roles.joinToString { it.asMention })
            .build()
        return EmbedBuilder().setTitle(title)
            .addField("Overview", overviewDescription, false)
            .addField("Members", membersDescription, true)
            .addField("Channels", channelsDescription, true)
            .addField("Roles", rolesDescription, true)
            .setThumbnail(this.iconUrl)
            .setFooter(footer, null)
            .setColor(color)
            .setTimestamp(Instant.now())
            .build()
    }
}
