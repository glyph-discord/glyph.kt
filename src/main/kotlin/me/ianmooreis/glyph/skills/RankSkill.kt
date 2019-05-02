/*
 * RankSkill.kt
 *
 * Glyph, a Discord bot that uses natural language instead of commands
 * powered by DialogFlow and Kotlin
 *
 * Copyright (C) 2017-2018 by Ian Moore
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

package me.ianmooreis.glyph.skills

import ai.api.model.AIResponse
import me.ianmooreis.glyph.directors.messaging.SimpleDescriptionBuilder
import me.ianmooreis.glyph.directors.skills.Skill
import me.ianmooreis.glyph.extensions.asPlainMention
import me.ianmooreis.glyph.extensions.reply
import me.ianmooreis.glyph.extensions.toDate
import me.ianmooreis.glyph.skills.utils.Hastebin
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant

/**
 * A skill that allows members to see bragging rights such as join order or account age
 */
object RankSkill : Skill("skill.rank", guildOnly = true) {
    override fun onTrigger(event: MessageReceivedEvent, ai: AIResponse) {
        event.channel.sendTyping().queue()
        val property: String? = ai.result.getStringParameter("memberProperty", null)
        if (property != null) {
            val members = event.guild.members
            when (property) {
                "join" -> event.message.reply(rankMembersByJoin(members, event.member))
                "created" -> event.message.reply(rankMembersByCreation(members, event.member))
                else -> event.message.reply("I'm not sure what property `$property` is for members.")
            }
        } else {
            event.message.reply("I'm not sure what the property you want to rank members by is.")
        }
    }

    private fun rankMembersByJoin(members: List<Member>, requester: Member): MessageEmbed {
        val rankedMembers = members.sortedBy { it.joinDate }
        return createRankEmbed("Guild Join Rankings", rankedMembers, requester) {
            "**${it.asPlainMention}** joined **${PrettyTime().format(it.joinDate.toDate())}** on **${it.joinDate}**"
        }
    }

    private fun rankMembersByCreation(members: List<Member>, requester: Member): MessageEmbed {
        val rankedMembers = members.sortedBy { it.user.idLong }
        return createRankEmbed("Account Creation Rankings", rankedMembers, requester) {
            "**${it.asPlainMention}** was created **${PrettyTime().format(it.user.creationTime.toDate())}** on **${it.user.creationTime}**"
        }
    }

    private fun createRankEmbed(
        title: String,
        rankedMembers: List<Member>,
        requester: Member,
        description: (Member) -> String
    ): MessageEmbed {
        val notable = SimpleDescriptionBuilder()
        val notableMembers = ArrayList<Member>()
        notableMembers.addAll(rankedMembers.take(3))
        notableMembers.addAll(rankedMembers.takeLast(3))
        notableMembers.forEach {
            notable.addField("`${rankedMembers.indexOf(it).plus(1)}.`", description(it))
        }
        val requesterRankDescription = "`${rankedMembers.indexOf(requester).plus(1)}.` ${description(requester)}"
        val embed = EmbedBuilder()
            .setTitle(title)
            .addField("Notable", notable.build(), false)
            .addField("You", requesterRankDescription, true)
            .setFooter("Rank", null)
            .setTimestamp(Instant.now())
        val everyone = SimpleDescriptionBuilder(true)
        rankedMembers.forEach {
            everyone.addField("${rankedMembers.indexOf(it).plus(1)}.", description(it).replace("*", ""))
        }
        Hastebin.postHasteBlocking(everyone.build(), 500).also {
            if (it !== null) {
                embed.addField("Everyone", "[Click to view]($it)", true)
            }
        }
        return embed.build()
    }
}