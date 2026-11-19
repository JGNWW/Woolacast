# Speler — voortgangsbalk en notering

Aanleiding: tijdens het afspelen ziet het blok onder de titel er raar uit. De golf
in de vulling is kort en strak (golflengte 12 dp, lijn 4 dp) en leest als een
gedraaid touw. Het witte handvat hoort niet bij de zalmkleurige vulling. De
notering zweeft als losse pil vlak boven de balk.

Vijf voorstellen, met de huidige versie als referentie. Podcast en hoes zijn
verzonnen; de kleuren komen uit een schermafbeelding van de app.

| | Balk | Notering |
| --- | --- | --- |
| Nu | korte golf, wit rond handvat | grijze pil boven de balk |
| 1 · Rustig | rechte lijn, handvat in de accentkleur | bovenregel boven de titel (zoals op de podcastpagina) |
| 2 · Lange golf | Material 3 Expressive zoals bedoeld: golflengte 40 dp, lage golf, opening bij een staafvormig handvat | omlijnde pil met meer ruimte |
| 3 · Eén regel | tijden links en rechts van een dikkere balk | badge op de hoes |
| 4 · Voortgangskaart | notering, balk en tijden in één vlak; rechts "nog 15 min" | bovenin de kaart |
| 5 · Dikke balk | balk van 10 dp zonder los handvat, groeit bij slepen | accentlink onder de showregel |

## Bestanden

- `build.py` maakt de HTML-schermen (`*.html`) en `overzicht.html`
- `render.mjs` rendert ze naar `renders/` (`NODE_PATH=$(npm root -g) node render.mjs`)
