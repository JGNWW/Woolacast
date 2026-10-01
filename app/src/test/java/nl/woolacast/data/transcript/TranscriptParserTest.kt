package nl.woolacast.data.transcript

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptParserTest {

    @Test
    fun `srt wordt per zin samengevoegd`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:02,500
            De nieuwe zaal gaat pas

            2
            00:00:02,600 --> 00:00:04,000
            in maart open.

            3
            00:00:04,500 --> 00:00:06,000
            Zijn jullie er klaar voor?
        """.trimIndent()
        val t = TranscriptParser.parse(srt, "application/x-subrip")
        assertTrue(t.timed)
        assertEquals(listOf("De nieuwe zaal gaat pas in maart open.", "Zijn jullie er klaar voor?"), t.lines.map { it.text })
        assertEquals(1_000L, t.lines[0].startMs)
        assertEquals(4_000L, t.lines[0].endMs)
    }

    @Test
    fun `vtt met sprekers en zonder uren`() {
        val vtt = """
            WEBVTT

            NOTE dit is een opmerking

            00:01.000 --> 00:03.000 align:start
            <v Sanne>Dit nummer schreven we zelf.</v>

            00:03.500 --> 00:05.000
            <v.loud Joost>En dan de band!</v>
        """.trimIndent()
        val t = TranscriptParser.parse(vtt, "text/vtt")
        assertEquals(listOf("Sanne", "Joost"), t.lines.map { it.speaker })
        assertEquals("En dan de band!", t.lines[1].text)
        assertEquals(3_500L, t.lines[1].startMs)
    }

    @Test
    fun `json per woord wordt een zin, een nieuwe spreker een nieuwe regel`() {
        val json = """
            {"version":"1.0.0","segments":[
              {"speaker":"Sanne","startTime":0.0,"endTime":0.4,"body":"Hallo"},
              {"speaker":"Sanne","startTime":0.4,"endTime":0.9,"body":"allemaal"},
              {"speaker":"Sanne","startTime":0.9,"endTime":1.0,"body":"."},
              {"speaker":"Joost","startTime":1.2,"endTime":1.6,"body":"Welkom"}
            ]}
        """.trimIndent()
        val t = TranscriptParser.parse(json, "application/json")
        assertEquals(listOf("Hallo allemaal.", "Welkom"), t.lines.map { it.text })
        assertEquals(listOf("Sanne", "Joost"), t.lines.map { it.speaker })
    }

    @Test
    fun `html heeft geen tijden`() {
        val t = TranscriptParser.parse("<p>Eerste alinea &amp; meer.</p><p>Tweede.</p>", "text/html")
        assertFalse(t.timed)
        assertEquals(listOf("Eerste alinea & meer.", "Tweede."), t.lines.map { it.text })
        assertEquals(-1, t.indexAt(5_000))
    }

    @Test
    fun `de regel die bij een tijd hoort`() {
        val t = TranscriptParser.parse("00:00:01,000 --> 00:00:02,000\nEen.\n\n00:00:05,000 --> 00:00:06,000\nTwee.", "text/srt")
        assertEquals(-1, t.indexAt(500))
        assertEquals(0, t.indexAt(3_000))
        assertEquals(1, t.indexAt(9_000))
    }
}
