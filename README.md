geo-publisher
=============

Lokaal is een draaiende docker(-machine) nodig om de images te bouwen.

Bouwen van docker images:
``./gradlew clean buildImage``

Deze images moeten handmatig naar docker hub gezet worden:
``docker push idgis/geopublisher_<module>:<tag>``

In de [geo-publisher-compose](https://github.com/IDgis/geo-publisher-compose) repo staan code en instructies om geo-publisher uit te rollen.
  
Voor de provider worden de artifacts gebouwd en naar Nexus gestuurd. 
Hier kan de [geo-publisher-deployment](https://github.com/IDgis/geo-publisher-deployment) repo mee verder
``./gradlew publisher-provider:clean publisher-provider:publish``
