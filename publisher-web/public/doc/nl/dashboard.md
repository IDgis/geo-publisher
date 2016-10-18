# Dashboard

## Algemeen

Het dashboard geeft een overzicht van de werking van de GIS publicatieomgeving.
Het bevat ook het menu om gegevens in te voeren en aan te passen.

### Onderdelen

Het menu bevindt zich aan de linkerkant.
Rechtsboven staat actuele meldingen over datasets en systeemtaken. 
Hier kan ook uitgelogd worden.
De onderste helft van de pagina is gereserveerd voor het overzicht (zie hieronder).
 
## Overzicht

Drie onderdelen die een overzicht geven van de actuele situatie.

### Gegevensbronnen
Welke gegevens bronnen zijn er en zijn zij verbonden met de publicatieomgeving. 

### Openstaande acties
Hier staan acties die nog niet afgerond zijn, zoals ...

### Systeemtaken 
Taken die momenteel lopen of in de afgelopen 24 uur zijn afgerond worden hier getoond.
De lijst op deze pagina is beperkt tot 5 meldingen. Voor een volledig overzicht wordt verwezen naar het [Systeemtaken logboek](logging/tasks.md).

## Workflow
Hier wordt een workflow beschreven waarmee in korte tijd een kaartlaag zichtbaar kan worden gemaakt in een GIS viewer.

### Stappen

* Kijk op het Dashboard of de gegevensbronnen zijn verbonden (OK).
* Ga naar [stijlen](styles/list.md) en maak ten minste 1 stijl aan.
* Ga naar [Brongegevens](datasources/list.md) en druk op de + knop om bij een brongegeven een dataset aan te maken.
* Neem bij de dataset de gegevens over en druk op ``Opslaan``. Importeer de data bij deze nieuwe dataset.
* Als het importeren gelukt is: Druk op `` + Nieuwe laag`` om een kaartlag voor deze data te maken.
* Voeg een stijl toe en druk op ``Opslaan``.
* Ga naar [Groepen](groups/list.md) en maak een nieuwe groep aan: ``+ maak een nieuwe groep``.
* Geef een naam voor deze groep en voeg de zojuist gemaakte laag toe. Druk op ``Opslaan``.
* Ga naar [Services](services/list.md) en maak een nieuwe service aan: ``+ maak een nieuwe service``.
* Geef een naam voor deze service en voeg de zojuist gemaakte groep toe. Druk op ``Opslaan``. 
* Kijk op het Dashboard of de systeemtaken voor dataset (import) en service (service) zijn uitgevoerd.
* Als alles goed is gegaan, ga dan terug naar de toegevoegde laag en klik op de preview knop. 
Een eenvoudige viewer laat de laag zien met de gekozen stijl.
* Ga naar de toegevoegde service en klik op de ``WMS`` knop. 
Neem de url over in een GIS view zoals QuantumGis om de service met zijn laag/lagen daarin zichtbaar te maken. 


[Inhoudsopgave](index.md)