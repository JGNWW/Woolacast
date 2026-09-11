package nl.woolacast.data.tips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * De koppen in deze test zijn echt: ze kwamen allemaal langs bij het bouwen van
 * de verzamelaar, en elk van hen ging een keer mis.
 */
class TipRulesTest {

    @Test
    fun `titel tussen aanhalingstekens wordt herkend`() {
        val headline = "De podcast ‘Bandsplain’ legt heel precies uit waarom Madonna groot werd"
        assertTrue("Bandsplain" in TipRules.candidates(headline))
    }

    @Test
    fun `een loze kop houdt de titel erachter niet tegen`() {
        // "Podcastrecensie" is zelf een kandidaat, maar mag de echte titel niet
        // verdringen: daar ging het eerder fout.
        val headline = "Podcastrecensie: ‘No such thing as a fish’ is duizelingwekkend"
        assertTrue("No such thing as a fish" in TipRules.candidates(headline))
    }

    @Test
    fun `een afkappingsquote is geen titel`() {
        // In "Afrika's" is de eerste sluitquote een apostrof. Zonder die regel
        // werd hier de show "Afrika" uit gelezen.
        val headline = "Nigeria maakt zich klaar voor ‘Afrika’s grootste beursgang ooit’"
        assertFalse(TipRules.candidates(headline).any { it.equals("Afrika", true) })
    }

    @Test
    fun `een medium dat zijn eigen aflevering aankondigt geeft geen tip`() {
        assertTrue(TipRules.ownAnnouncement("SZ-Podcast: Hey München", "SZ", "Süddeutsche Zeitung"))
        assertTrue(TipRules.ownAnnouncement("Norwich City: Podcast", "BBC", "BBC Radio Norfolk"))
        assertFalse(
            TipRules.ownAnnouncement(
                "De podcast ‘Bandsplain’ legt uit waarom Madonna groot werd",
                "Trouw",
                "The Ringer"
            )
        )
    }

    @Test
    fun `dezelfde uitgever als het medium telt als eigen huis`() {
        assertTrue(TipRules.sameHouse("The Guardian", "The Guardian"))
        assertTrue(TipRules.sameHouse("VPRO", "VPRO Radio"))
        assertFalse(TipRules.sameHouse("Trouw", "The Ringer"))
        // "De" en "podcast" zijn te algemeen om iets te bewijzen.
        assertFalse(TipRules.sameHouse("De Standaard", "De Correspondent"))
    }

    @Test
    fun `alleen een show die exact zo heet telt`() {
        assertTrue(TipRules.titleMatches("Serial", "Serial"))
        assertTrue(TipRules.titleMatches("Proces X", "Proces X: Seriedoders"))
        assertTrue(TipRules.titleMatches("Signal Hill", "Signal Hill - de podcast"))
        assertFalse(TipRules.titleMatches("Laika", "Laika en de sterren"))
        assertFalse(TipRules.titleMatches("Afrika", "Afrika Nu"))
    }

    @Test
    fun `titels worden vergeleken zonder opmaak`() {
        assertEquals(TipRules.normalise("Rocco, Sophia en de Zwarte Dood"),
            TipRules.normalise("rocco sophia en de zwarte dood"))
    }

    @Test
    fun `een kop zonder podcastwoord is geen tip`() {
        assertFalse(TipRules.mentionsPodcast("Vijf tips voor een zuinigere cv-ketel"))
        assertTrue(TipRules.mentionsPodcast("Vijf podcasttips: van camping tot cruise"))
        assertTrue(TipRules.mentionsPodcast("Veckans podd: en historia om makt"))
    }

    @Test
    fun `een tipwoord herkennen we in meerdere talen`() {
        assertTrue(TipRules.mentionsTip("De beste kinderpodcasts van dit moment"))
        assertTrue(TipRules.mentionsTip("Die besten Podcasts der Woche"))
        assertTrue(TipRules.mentionsTip("Los mejores podcasts de la semana"))
        assertTrue(TipRules.mentionsTip("best podcasts of the week"))
        assertFalse(TipRules.mentionsTip("Nieuw presentatieduo voor Europa draait door"))
    }

    @Test
    fun `de naam moet in dezelfde adem als het woord podcast staan`() {
        val goed = "Miles Davis, een eeuweling die nooit verveelt, leert podcast ‘Miles. Badass & jazzicoon’"
        assertTrue(TipRules.nearPodcast(goed, "Miles. Badass & jazzicoon"))
        // Deze kop kwam binnen op een zoekopdracht over podcasts, maar gaat over
        // een film. Zonder het woord in de kop zelf is er niets om op te gaan.
        val fout = "Professor klassieke mythologie leidt in Kinepolis ‘The Odyssey’ in"
        assertFalse(TipRules.mentionsPodcast(fout))
        assertFalse(TipRules.nearPodcast(fout, "The Odyssey"))
    }

    @Test
    fun `de opsomming onder een tiplijst levert de namen`() {
        val tekst = "Vijf nieuwe podcasttips: van onderwereld tot regenboog. " +
            "Met deze week: Luister Anita, Dan Taberski's Manifesto, Proces X: Seriedoders, " +
            "De onderwereld en Radio Boos: Grote gesprekken."
        val namen = TipRules.listCandidates(tekst)
        assertTrue("Luister Anita" in namen)
        assertTrue("Proces X: Seriedoders" in namen)
        assertTrue("De onderwereld" in namen)
    }

    @Test
    fun `de naam achter het woord podcast wordt woord voor woord langer geprobeerd`() {
        val tekst = "In de podcast Mijn taalmaatje wil naar huis volgt Laura Hesselink de Syrische Nedal"
        val namen = TipRules.afterWordCandidates(tekst)
        assertTrue("Mijn taalmaatje" in namen)
        assertTrue("Mijn taalmaatje wil naar huis" in namen)
    }

    @Test
    fun `een titel die we al kennen hoeft niet geraden te worden`() {
        assertTrue(TipRules.titleIn("De podcast \u2018Bandsplain\u2019 legt uit", "Bandsplain"))
        assertTrue(TipRules.titleIn("Proces X gaat over seriemoordenaars", "Proces X: Seriedoders"))
        assertFalse(TipRules.titleIn("Serialiseren is een werkwoord", "Serial"))
    }

    @Test
    fun `rommel uit de opmaak van een pagina is geen titel`() {
        assertFalse(TipRules.looksLikeTitle("Lees meer"))
        assertFalse(TipRules.looksLikeTitle("PODCASTS"))
        assertFalse(TipRules.looksLikeTitle("de"))
        assertFalse(TipRules.looksLikeTitle("Waarom gaan de leerprestaties van Nederlandse scholieren zo hard achteruit"))
        assertTrue(TipRules.looksLikeTitle("Helden op Pootjes"))
    }
}
