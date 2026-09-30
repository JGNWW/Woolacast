package nl.woolacast.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MakersTest {

    @Test
    fun `het eerste deel van een samengestelde naam is de maker`() {
        assertEquals("NPO Luister", Makers.name("NPO Luister / BNNVARA"))
        assertEquals("Dag en Nacht", Makers.name("Dag en Nacht | Podimo"))
        assertEquals("Podimo", Makers.name("Podimo & Alexander Klöpping"))
    }

    @Test
    fun `een gewone naam blijft heel`() {
        assertEquals("BNR Nieuwsradio", Makers.name("BNR Nieuwsradio"))
        assertEquals("NRC", Makers.name("  NRC "))
    }

    @Test
    fun `een en-teken in een naam zonder spaties splitst niet`() {
        // "AT&T" is één naam; alleen " & " scheidt twee makers.
        assertEquals("AT&T Studios", Makers.name("AT&T Studios"))
    }

    @Test
    fun `de sleutel negeert hoofdletters en leestekens`() {
        assertEquals(Makers.key("De Volkskrant"), Makers.key("de volkskrant."))
        assertEquals("npoluister", Makers.keyOf("NPO Luister / AVROTROS"))
    }
}
