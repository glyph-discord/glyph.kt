/*
 * FandomExtractorTest.kt
 *
 * Glyph, a Discord bot that uses natural language instead of commands
 * powered by DialogFlow and Kotlin
 *
 * Copyright (C) 2017-2020 by Ian Moore
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

package skills.wiki

import kotlinx.coroutines.runBlocking
import me.ianmooreis.glyph.skills.wiki.FandomExtractor
import org.junit.jupiter.api.Test

internal class FandomExtractorTest {
    private val extractor = FandomExtractor("masseffect", 0)

    @Test
    fun getRealArticle() = runBlocking {
        val garrus = extractor.getArticle("Garrus")

        assert(garrus != null)

        if (garrus != null) {
            assert(garrus.title == "Garrus Vakarian")
            assert(garrus.abstract.startsWith("Garrus Vakarian is a turian"))
            assert(garrus.thumbnail != null)
        }
    }

    @Test
    fun getFakeArticle() = runBlocking {
        val fake = extractor.getArticle("realsnotreal")

        assert(fake == null)
    }
}