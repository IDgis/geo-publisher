Lagen
=====
> In een vorige stap heeft u een dataset aangemaakt. In deze stap gaat u een laag aanmaken en de stijl aanpassen (optioneel) de laag controleren en publiceren.
### Laag aanmaken

1) Klik links bij "Databeheer" op **Datasets**.
2) Klik bij uw dataset op de knop **+ nieuwe laag**
3) Vul de velden in. Bij trefwoorden vult u zelf gekozen trefwoorden in om de vindbaarheid van de laag te vergroten, klik op de **+ knop** om het trefwoord toe te voegen. 
4) Kies een stijl in dit geval een default style en klik op de **+ knop** om de stijl toe te voegen. Klik buiten keuzevenster om verder te gaan. Het aanpassen van de stijl wordt in de volgende stap uitgelegd. Het aanzetten van tiling is voor geavanceerd gebruik en kunt u nu overslaan.
5) Klik op **Opslaan**.

### Stijl aanpassen via QGIS (optioneel)

> In deze stap ziet u hoe u met behulp van GGIS (desktop GIS) een stijl kunt aanpassen. QGIS is open source software die u gratis kunt downloaden en gebruiken (zie [http://www.qgis.org/nl/](http://www.qgis.org/nl/)).

1) Klik links bij Databeheer op **Datasets**.
2) Klik bij de eerder toegevoegde dataset op de downloadicon voor de metadata van de dataset. De bestandsbeschrijving wordt in een nieuwe tab geopend.
3) Klik op de tab/knop **Details**
4) Kopieer de URL achter OGC:WFS. 
5) Start QGIS en voeg onder **kaartlagen** een nieuwe **WFS laag** toe mbv de zojuist gekopieerde URL. 
6) Zorg dat "paneel lagen" open staat en open van daaruit het paneel voor opmaak van stijlen 
7) Selecteer een van de kolommen om je stijl op te baseren en gebruik bijvoorbeeld vervolgens een kleurverloop (of "willekeurige kleur")
8) Klik onderaan op Style opslaan als **SLD**.
9) Ga naar de **GeoPublisher** en klik links bij "servicebeheer" op **Stijlen**.
10) Klik op **Maak een nieuwe stijl ..**
11) Upload de stijl via de knop **choose file** en klik op **Gekozen stijl inladen** of copy paste de stijl via een texteditor
12) Geef aan of het om punten, lijnen, vlakken gaat en valideer eventueel de stijl tegen het schema
13) Klik op **stijl opslaan** (stijl wordt automatisch gevalideerd)
14) U kunt de stijl nu toepassen in de stap **Kies een stijl ..** (zie boven).  
15) **NB Publiceer de service opnieuw om de stijl te effectueren.** 

### Laag controleren en services publiceren

1) Klik links bij "Servicebeheer" op **Lagen**.
2) Klik op de laag die u heeft toegevoegd.
3) Onderaan ziet u nu dat de "preview" knop beschikbaar is. Klik op de knop om de laag in preview modus  te bekijken (opent in nieuw tabblad)
4) Klik links bij "Servicebeheer" op **Services** en vervolgens op **Publiceren**.  



[Inhoudsopgave](../index.md)