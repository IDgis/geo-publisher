<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gmd="http://www.isotc211.org/2005/gmd"
  xmlns:srv="http://www.isotc211.org/2005/srv"
  xmlns:gco="http://www.isotc211.org/2005/gco"
  xmlns:gml="http://www.opengis.net/gml"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xlink="http://www.w3.org/1999/xlink">
<!--
 An xsl template for displaying GeoSticker ISO metadata in ArcCatalog.
 Copyright (c) 2007-2009, ESRI Nederland B.V.
-->
<xsl:output method="html" version="4.0" encoding="iso-8859-1" indent="yes" omit-xml-declaration="yes"/>
<!-- GeoportalUrl must end with a /, for example http://portal/geoportal/ -->
<xsl:variable name="GeoportalUrl"></xsl:variable>
<xsl:template match="/">
<html>
  <head> 
<P STYLE="text-align:left; margin-left:7.7in; margin-top:0.2in;"><img src="http://gisopenbaar.overijssel.nl/GeoPortal/MIS4GIS/logo.png"/></P>
<!--<img src="/GeoPortal/MIS4GIS/logo.png"; align=right/>-->
    <style>
      BODY {margin-top:0in; margin-left:0.5in; background-image:url("http://gisopenbaar.overijssel.nl/GeoPortal/MIS4GIS/logo1.png");text-align:left;}
      BODY {font-size:10pt; font-family:Verdana,sans-serif}
      
      .DIVNoISO {text-align:center; color:#6495ED; margin-left:.5in; margin-right:.5in}
      <!-- tabs -->
      TABLE {border-collapse:collapse}
      TABLE.tabs  {position:relative; top:0; valign:top; table-layout:fixed; text-align:center}
      .tun, .tsel, .tover {font-weight:bold; font-size:9pt; border: 0pt; border-color:#FFFFFF; color:#FFFFFF}
      <!-- selected tab -->
      .tsel  {background-color:#7B0018}
      <!-- unselected tab -->
      .tun   {background-color:#009EEF}
      <!-- unselected tab hilite -->
      .tover {background-color:#7B0018; cursor:hand}
      <!-- properties box -->
      @media screen
      <!-- BORDER PAGINA-->
      {
      .f   {border:'1pt solid #A50821';
            c; position:relative; top:0; width:946; height:686;}
      }
      <!-- property value -->
      .pv,.pvActive, .pv TD,.pvActive TD  {font-family:Verdana,sans-serif; color:#202020}
      TD {font-size:10pt}
      .pv,.pvActive {line-height:135%; margin:0in 0.15in 0.75in 0.15in}
      @media screen { .pv {display:none} }
      <!-- property name -->
      .pn  {margin-left:0in; color:#202020; font-weight:normal}
      <!-- property name indented -->
      .pni,.pniHidden  {margin-left:0.2in; color:#202020; font-weight:normal}
      .pni td {padding: 0 0.05in}
      @media screen {.pniHidden {display:none}}
      <!-- group heading -->
      .ph1  {margin-left:0in; color:#009EEF; font-weight:bold; cursor:}

      <!-- title -->
     .h1   {position:relative; top:-110; text-align:left;
            font-weight:bold; font-size:20; font-family:Verdana,sans-serif; color:#A50821}
      <!-- data type -->
      .h2   {position:relative; top:0; text-align:center;
            font-size:15; font-family:Verdana,sans-serif; color:#0A4786}
      <!-- naam dataset -->
      .h3   {position:relative; top:0; text-align:center;
            font-size:13; font-family:Verdana,sans-serif; color:#000000}
      <!-- property name -->
      .pnn  {margin-left:0in; color:#009EEF; font-weight:normal}
      <!-- property name bold -->
      .pnnb  {margin-left:0in; color:#009EEF; font-weight:bold}
      <!-- group heading indented -->
      .ph2  {margin-left:0.1in; color:#009EEF; font-weight:bold; cursor:}
      <!-- group heading hilite -->
      .pover1 {margin-left:0in; color:#A50821; font-weight:bold; cursor:hand}
      <!-- group heading hilite indented -->
      .pover2 {margin-left:0.1in; color:#009EEF; font-weight:bold; cursor:hand}
      <!-- No information -->
      .pinfo {text-align:center; color:#0A4786}

     .tabname {font-size:15; font-weight:bold; font-family:Verdana,sans-serif; color:#0A4786;border-top:1.5pt solid #A9BECF;;border-bottom:1.5pt solid #A9BECF}

      @media print
      {
      .tabs {display:none}
      .DIVNoISO,.h1,.h2,.h3,.pinfo {text-align:left}
      }
      @media screen
      {
      .tabname {display:none}
      }
    </style>
    <SCRIPT LANGUAGE="JScript">
      <xsl:comment>
        <![CDATA[
          function doHilite()  {
            var e = window.event.srcElement;
            if (e.className == "tun") {
              e.className = "tover";
            }
            else if (e.className == "tover") {
              e.className = "tun";
            }
            else if (e.className == "ph1") {
              e.className = "pover1";
            }
            else if (e.className == "ph2") {
              e.className = "pover2";
            }
            else if (e.className == "pover1") {
              e.className = "ph1";
            }
            else if (e.className == "pover2") {
              e.className = "ph2";
            }
            window.event.cancelBubble = true;
          }

          function changeTab(eRow)  {
            var tabs = eRow.cells;
            for (var i = 0; i < tabs.length; i++) {
              var oldTab = tabs[i];
              if (oldTab.className == "tsel") {
                break;
              }
            }
            oldTab.className = "tun";
            var oldContent = getAssociated(oldTab);
            oldContent.className = "pv";

            var newTab = window.event.srcElement;
            newTab.className = "tsel";
            var newContent = getAssociated(newTab);
            newContent.className = "pvActive";

            window.event.cancelBubble = true;
          }

          function hideShowGroup(e)  {
            var theGroup = e.children[0];
            if(theGroup.className=="pni")
              theGroup.className="pniHidden";
            else
              theGroup.className="pni";

            window.event.cancelBubble = true;
          }

          function getAssociated(e) {
            if (e.tagName == "TD") {
              switch (e.id) {
                case "IdentTab":
                  return (Identificatie);
                case "ServiceTab":
                  return (Service);
                case "ContactTab":
                  return (Contact);
                case "MetaTab":
                  return (Metametadata);
                case "DekkingTab":
                  return (Dekking);
                case "KwaliteitTab":
                  return (Kwaliteit);
                case "InhoudTab":
                  return (Inhoud);
                case "DistTab":
                  return (Distributie);
              }
            }
          }

          function initTab() {
            var e = document.all("Identificatie");
            if (e != null) {
              e.className = "pvActive";
            }
          }
        ]]>
      </xsl:comment>
    </SCRIPT>
  </head>
  <body onload="initTab()">
    <xsl:choose>
      <xsl:when test="/gmd:MD_Metadata or /metadata/gmd:MD_Metadata">
        <xsl:apply-templates select="//gmd:MD_Metadata[1]"/>
      </xsl:when>
      <xsl:otherwise>
        <DIV class="DIVNoISO">
          <br/><br/>
          Dit document bevat geen informatie die kan worden bekeken met de GeoSticker Stylesheet.<br/>
          Maak eerst Nederlandse (ISO) metadata aan met behulp van de GeoSticker Editor.
        </DIV>
      </xsl:otherwise>
    </xsl:choose>
  </body>
</html>
</xsl:template>

<!-- MD_Metadata -->
<xsl:template match="gmd:MD_Metadata">
  <TABLE class="tabs" cols="7" frame="void" rules="cols" width="315" height="28">
    <COL WIDTH="135"/>
    <COL WIDTH="135"/>
    <COL WIDTH="135"/>
    <COL WIDTH="135"/>
    <COL WIDTH="135"/>
    <COL WIDTH="135"/>
    <COL WIDTH="136"/>
    <TR height="35" onmouseover="doHilite()" onmouseout="doHilite()" onmousedown="changeTab(this)">
      <TD ID="IdentTab" CLASS="tsel" TITLE="Klik hier voor informatie over de identificatie">Identificatie</TD>
      <xsl:if test="gmd:identificationInfo/srv:SV_ServiceIdentification"><TD ID="ServiceTab" CLASS="tun" TITLE="Klik hier voor informatie over de service">Service</TD></xsl:if>
      <TD ID="ContactTab" CLASS="tun" TITLE="Klik hier voor contactinformatie">Contacten</TD>
      <TD ID="MetaTab" CLASS="tun" TITLE="Klik hier voor de metametadata">Metametadata</TD>
      <TD ID="DekkingTab" CLASS="tun" TITLE="Klik hier voor informatie over dekking in ruimte en tijd">Dekking</TD>
      <TD ID="KwaliteitTab" CLASS="tun" TITLE="Klik hier voor informatie over de kwaliteit">Kwaliteit</TD>
      <TD ID="InhoudTab" CLASS="tun" TITLE="Klik hier voor informatie over de inhoud">Inhoud</TD>
      <TD ID="DistTab" CLASS="tun" TITLE="Klik hier voor distributiegegevens">Distributie</TD>
    </TR>
  </TABLE>
  <DIV ID="Group" CLASS="f">
    <!-- Identificatie Tab -->
    <DIV ID="Identificatie" class="pv">
      <p class="tabname">Identificatie</p>
      <xsl:choose>
        <xsl:when test="not((normalize-space(gmd:identificationInfo/*/gmd:citation)!='')
                or (normalize-space(gmd:identificationInfo/*/gmd:abstract)!='')
                or (normalize-space(gmd:identificationInfo/*/gmd:purpose)!='')
                or (normalize-space(gmd:identificationInfo/*/gmd:resourceConstraints)!='')
                or (normalize-space(gmd:identificationInfo/*/gmd:graphicOverview)!=''))">
          <DIV CLASS="pinfo">
            <BR/>
            Geen identificatie informatie beschikbaar.<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <BR/>
          <!-- Thumbnail ArcInfo 8.1 en hoger -->
          <xsl:apply-templates select="/metadata/Binary/Thumbnail[img/@src!='']"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:citation"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:status"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:abstract"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceSpecificUsage"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:purpose"/>
          <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialResolution"/>
          <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialRepresentationType"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceMaintenance/gmd:MD_MaintenanceInformation"/>
          <xsl:if test="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:topicCategory!=''">
            <BR/>
            <B>Onderwerpen:</B>
            <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:topicCategory"/>
          </xsl:if>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords"/>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:graphicOverview"/>
          <xsl:if test="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:supplementalInformation!=''">
            <BR/>
            <B>Aanvullende informatie:</B>
            <BR/>
            <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:supplementalInformation"/>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:aggregationInfo!=''">
            <B>Gerelateerde datasets:</B>
            <xsl:apply-templates select="gmd:identificationInfo/*/gmd:aggregationInfo"/>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/*!=''">
            <BR/>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_Constraints!=''">
            <B>Gebruiksbeperkingen:</B>
            <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_Constraints"/>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:useConstraints">
            <B>(Juridische) gebruiksrestricties:</B>
            <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:useConstraints"/>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:otherConstraints/gco:CharacterString='no limitation on public access'">
            <B>Toegangsrestricties:</B>
            <DIV CLASS="pni">Geen</DIV>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:accessConstraints/@gco:nilReason!=''">
            <B>(Juridische) toegangsrestricties:</B>
            <DIV CLASS="pni">Geen</DIV>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:accessConstraints/gmd:MD_RestrictionCode/@codeListValue!='otherRestrictions'">
            <B>(Juridische) toegangsrestricties:</B>
            <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:accessConstraints"/>
          </xsl:if>
          <xsl:if test="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:otherConstraints!=''">
            <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:otherConstraints"/>
          </xsl:if>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_SecurityConstraints"/>
          <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:language"/>
          <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:characterSet"/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
    <!-- Service Tab -->
    <DIV ID="Service" class="pv">
      <p class="tabname">Service</p>
      <xsl:apply-templates select="gmd:identificationInfo/srv:SV_ServiceIdentification"/>
    </DIV>
    <!-- Contacten Tab -->
    <DIV ID="Contact" class="pv">
      <p class="tabname">Contact</p>
      <xsl:choose>
        <xsl:when test="not(gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString!='')">
          <DIV CLASS="pinfo">
            <BR/>
            Geen contact informatie beschikbaar.<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="gmd:identificationInfo/*/gmd:pointOfContact"/>
          <BR/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
    <!-- Metametadata Tab -->
    <DIV ID="Metametadata" CLASS="pv">
      <p class="tabname">Metametadata</p>
      <xsl:choose>
        <xsl:when test="not((gmd:contact!='') or (gmd:dateStamp!=''))">
          <DIV CLASS="pinfo">
            <BR/>
            Geen informatie beschikbaar over de metadata.<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="gmd:contact"/>
          <xsl:apply-templates select="gmd:language"/>
          <xsl:apply-templates select="gmd:characterSet"/>
          <xsl:apply-templates select="gmd:hierarchyLevel"/>
          <xsl:apply-templates select="gmd:hierarchyLevelName"/>
          <xsl:apply-templates select="gmd:dateStamp"/>
          <xsl:apply-templates select="gmd:metadataStandardName"/>
          <xsl:apply-templates select="gmd:metadataStandardVersion"/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
    <!-- Dekking Tab -->
    <DIV ID="Dekking" class="pv">
      <p class="tabname">Dekking</p>
      <xsl:choose>
        <xsl:when test="not((gmd:referenceSystemInfo!='') or (gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent!='') or (gmd:identificationInfo/srv:SV_ServiceIdentification/srv:extent/gmd:EX_Extent!=''))">
          <DIV CLASS="pinfo">
            <BR/>
            Geen informatie beschikbaar over de dekking.<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <BR/>
          <xsl:apply-templates select="gmd:referenceSystemInfo[1]"/>
          <xsl:apply-templates select="gmd:referenceSystemInfo[2]"/>
          <xsl:apply-templates select="gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent|gmd:identificationInfo/srv:SV_ServiceIdentification/srv:extent/gmd:EX_Extent"/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
    <!-- Kwaliteit Tab -->
    <DIV ID="Kwaliteit" class="pv">
      <p class="tabname">Kwaliteit</p>
      <xsl:choose>
        <xsl:when test="not((gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString!='')
                or (gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_CompletenessOmission/gmd:result/gmd:DQ_QuantitativeResult/gmd:value!='')
                or (gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_AbsoluteExternalPositionalAccuracy/gmd:result/gmd:DQ_QuantitativeResult/gmd:value!='')
                or (gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString!='')
                or (gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:processStep!='')
                or (gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:source!=''))">
          <DIV CLASS="pinfo">
            <BR/>
            Geen informatie beschikbaar over de kwaliteit.<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="gmd:dataQualityInfo"/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
    <!-- Inhoud Tab (datamodel) -->
    <DIV ID="Inhoud" class="pv">
      <p class="tabname">Inhoud</p>
      <xsl:choose>
        <xsl:when test="not((gmd:applicationSchemaInfo!='') or (/metadata/FC_FeatureCatalogue!=''))">
          <DIV CLASS="pinfo">
            <BR/>
            Geen inhoudelijke informatie beschikbaar (applicatieschema).<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="gmd:applicationSchemaInfo"/>
          <xsl:apply-templates select="/metadata/FC_FeatureCatalogue"/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
    <!-- Distributie Tab -->
    <DIV ID="Distributie" class="pv">
      <p class="tabname">Distributie</p>
      <xsl:choose>
        <xsl:when test="not(gmd:distributionInfo/gmd:MD_Distribution//gco:CharacterString!='' or gmd:distributionInfo/gmd:MD_Distribution//@codeListValue!='')">
          <DIV CLASS="pinfo">
            <BR/>
            Geen distributie informatie beschikbaar.<BR/>
          </DIV>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact"/>
          <xsl:apply-templates select="gmd:distributionInfo/gmd:MD_Distribution"/>
          <xsl:apply-templates select="gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions"/>
          <xsl:apply-templates select="gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributionOrderProcess"/>
        </xsl:otherwise>
      </xsl:choose>
    </DIV>
  </DIV>
</xsl:template>

<!-- Metadata auteur -->
<xsl:template match="gmd:MD_Metadata/gmd:contact[gmd:CI_ResponsibleParty]">
  <BR/>
  <B>Metadata auteur:</B>
  <DIV CLASS="pni">
    <xsl:if test="gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString!=''">
      <DIV CLASS="pn"><B>Naam organisatie: </B><xsl:value-of select="gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString"/><BR/></DIV>
    </xsl:if>
    <xsl:apply-templates select="gmd:CI_ResponsibleParty"/>
  </DIV>
</xsl:template>

<!-- Metametadata -->
<xsl:template match="gmd:MD_Metadata/gmd:language">
  <BR/>
  <xsl:variable name="language" select="."/>
  <xsl:if test="(gco:CharacterString!='') or (gmd:LanguageCode!='')">
    <B>Metadata taal: </B>
    <xsl:choose>
      <xsl:when test="contains($language, 'dut')">Nederlands<BR/>
      </xsl:when>
      <xsl:when test="contains($language, 'eng')">Engels<BR/>
      </xsl:when>
      <xsl:when test="contains($language, 'en')">Engels<BR/>
      </xsl:when>
      <xsl:when test="contains($language, 'deu')">Duits<BR/>
      </xsl:when>
      <xsl:when test="contains($language, 'fre')">Frans<BR/>
      </xsl:when>
      <xsl:when test="contains($language, 'spa')">Spaans<BR/>
      </xsl:when>
      <xsl:when test="contains($language, 'fry')">Fries<BR/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="."/><BR/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:characterSet">
  <xsl:if test="gco:CharacterString!=''">
    <B>Metadata karakterset: </B><xsl:value-of select="gmd:MD_CharacterSetCode/@codeListValue"/><BR/>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:hierarchyLevel">
  <xsl:variable name="hierarchyLevel" select="gmd:MD_ScopeCode/@codeListValue"/>
  <xsl:if test=".!=''">
    <B>Metadata hiërarchieniveau: </B>
    <xsl:choose>
      <xsl:when test="contains($hierarchyLevel, 'series')">Dataset serie</xsl:when>
      <xsl:when test="contains($hierarchyLevel, 'dataset')">Dataset</xsl:when>
      <xsl:when test="contains($hierarchyLevel, 'service')">Service</xsl:when>
      <xsl:otherwise>Onbekend: <xsl:value-of select="$hierarchyLevel"/></xsl:otherwise>
    </xsl:choose>
    <BR/>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:hierarchyLevelName">
  <xsl:if test=".!=''">
    <B>Beschrijving hierarchisch niveau: </B><xsl:value-of select="gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:dateStamp">
  <xsl:if test=".!=''">
    <BR/>
    <B>Metadata wijzigingsdatum: </B><xsl:value-of select="gco:Date"/><BR/>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:metadataStandardName">
  <xsl:if test=".!=''">
    <B>Metadata standaard naam: </B><xsl:value-of select="gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:metadataStandardVersion">
  <xsl:if test=".!=''">
    <B>Metadata standaard versie: </B><xsl:value-of select="gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Thumbnail ArcInfo 8.1 en hoger-->
<xsl:template match="/metadata/Binary/Thumbnail[img/@src!='']">
  <p align="center"><IMG ID="thumbnail" SRC="{img/@src}" STYLE="height:144; border:'2 outset #FFFFFF'"/></p>
</xsl:template>

<!-- Identificatie -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation[gmd:CI_Citation!='']">
  <xsl:if test="gmd:CI_Citation/gmd:title/gco:CharacterString!=''">
    <DIV CLASS="h1"><xsl:value-of select="gmd:CI_Citation/gmd:title/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:CI_Citation/gmd:alternateTitle/gco:CharacterString!=''">
    <B>Alternatieve titel: </B><xsl:value-of select="gmd:CI_Citation/gmd:alternateTitle/gco:CharacterString"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:CI_Citation/gmd:edition/gco:CharacterString!=''">
    <B>Versie:</B><xsl:value-of select="gmd:CI_Citation/gmd:edition/gco:CharacterString"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:CI_Citation/gmd:identifier/gmd:MD_Identifier/gmd:code/gco:CharacterString!=''">
    <B>Unieke Identifier: </B><xsl:value-of select="gmd:CI_Citation/gmd:identifier/gmd:MD_Identifier/gmd:code/gco:CharacterString"/>
    <xsl:if test="$GeoportalUrl!=''">
      <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
      <a href="{$GeoportalUrl}rest/find/document?searchText=serviceDatasetIdentifier:{gmd:CI_Citation/gmd:identifier/gmd:MD_Identifier/gmd:code/gco:CharacterString}&amp;f=html">Zoek services</a>
    </xsl:if><BR/>
  </xsl:if>
  <xsl:if test="gmd:CI_Citation/gmd:series/gmd:CI_Series/gmd:name/gco:CharacterString!=''">
    <B>Serienaam/-nummer: </B><xsl:value-of select="gmd:CI_Citation/gmd:series/gmd:CI_Series/gmd:name/gco:CharacterString"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:CI_Citation/gmd:date!=''">
    <BR/>
    <xsl:for-each select="gmd:CI_Citation/gmd:date">
      <xsl:if test="gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='creation'">
        <B>Datum voltooiing: </B><xsl:value-of select="gmd:CI_Date/gmd:date/gco:Date"/><BR/>
      </xsl:if>
      <xsl:if test="gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='publication'">
        <B>Datum publicatie: </B><xsl:value-of select="gmd:CI_Date/gmd:date/gco:Date"/><BR/>
      </xsl:if>
      <xsl:if test="gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='revision'">
        <B>Datum laatste wijziging: </B><xsl:value-of select="gmd:CI_Date/gmd:date/gco:Date"/><BR/>
      </xsl:if>
    </xsl:for-each>
    <BR/>
  </xsl:if>
</xsl:template>

<!-- Samenvatting -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:abstract">
  <xsl:if test="gco:CharacterString!=''">
    <B>Samenvatting: </B><xsl:value-of select="gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Doel van vervaardiging -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:purpose">
  <xsl:if test="gco:CharacterString!=''">
    <B>Doel van vervaardiging: </B><xsl:value-of select="gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Status -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:status">
  <xsl:variable name="status" select="gmd:MD_ProgressCode/@codeListValue"/>
  <xsl:if test="gmd:MD_ProgressCode/@codeListValue!=''">
    <B>Status: </B>
    <xsl:choose>
      <xsl:when test="contains($status, 'completed')">Compleet<BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'complete')">Compleet<a href="javascript:" title="Code foutief gespeld in XML" style="color: red; text-decoration: none">*</a><BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'historicalArchive')">Historisch archief<BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'obsolete')">Niet langer relevant<BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'onGoing')">Continu geactualiseerd<BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'planned')">Gepland<BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'required')">Actualisatie vereist<BR/>
      </xsl:when>
      <xsl:when test="contains($status, 'underDevelopment')">In ontwikkeling<BR/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="gco:CharacterString"/><BR/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<!-- Herzieningsfrequentie -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceMaintenance/gmd:MD_MaintenanceInformation">
  <xsl:variable name="herzieningsfrequentie" select="gmd:maintenanceAndUpdateFrequency/gmd:MD_MaintenanceFrequencyCode/@codeListValue"/>
  <xsl:if test="gmd:maintenanceAndUpdateFrequency/gmd:MD_MaintenanceFrequencyCode/@codeListValue!=''">
    <B>Herzieningsfrequentie: </B>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="contains($herzieningsfrequentie, 'continual')">Continu<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'daily')">Dagelijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'weekly')">Wekelijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'fortnightly')">2-wekelijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'monthly')">Maandelijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'quarterly')">1 x per kwartaal<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'biannually')">1 x per half jaar<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '2annually')">2-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '3annually')">3-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '4annually')">4-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '5annually')">5-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '6annually')">6-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '7annually')">7-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '8annually')">8-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '9annually')">9-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'moreThan10annually')">Meer dan 10-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, '10annually')">10-jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'annually')">Jaarlijks<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'asNeeded')">Indien nodig<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'irregular')">Onregelmatig<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'notPlanned')">Niet gepland<BR/></xsl:when>
    <xsl:when test="contains($herzieningsfrequentie, 'unknown')">Onbekend<BR/></xsl:when>
    <xsl:otherwise><xsl:value-of select="gco:CharacterString"/><BR/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:if test="gmd:dateOfNextUpdate/gco:Date!=''">
    <B>Datum volgende herziening: </B><xsl:value-of select="gmd:dateOfNextUpdate/gco:Date"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Trefwoorden -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords">
  <xsl:if test="gmd:keyword/gco:CharacterString!=''">
    <B>Trefwoorden:</B>
    <xsl:for-each select="gmd:keyword/gco:CharacterString">
      <DIV CLASS="pni"><xsl:value-of select="."/><BR/></DIV>
    </xsl:for-each>
    <xsl:if test="gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString!=''">
      <B>Thesaurus trefwoorden: </B><xsl:value-of select="gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString"/><BR/>
    </xsl:if>
    <xsl:if test="gmd:thesaurusName/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date!=''">
      <B>Publicatiedatum thesaurus: </B><xsl:value-of select="gmd:thesaurusName/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date"/><BR/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<!-- Potentieel gebruik -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceSpecificUsage">
  <xsl:if test="gmd:MD_Usage/gmd:specificUsage/gco:CharacterString!=''">
    <B>Potentieel gebruik: </B><xsl:value-of select="gmd:MD_Usage/gmd:specificUsage/gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Toepassingsschaal -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialResolution">
  <xsl:if test="gmd:MD_Resolution/gmd:equivalentScale/gmd:MD_RepresentativeFraction/gmd:denominator!='' and
    gmd:MD_Resolution/gmd:equivalentScale/gmd:MD_RepresentativeFraction/gmd:denominator/gco:Integer!='0'">
    <B>Toepassingsschaal: </B>1:<xsl:value-of select="gmd:MD_Resolution/gmd:equivalentScale/gmd:MD_RepresentativeFraction/gmd:denominator/gco:Integer"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Voorbeeldplaatje -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:graphicOverview">
  <xsl:if test="gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString!=''">
    <BR/><B>Voorbeeld: </B><A TARGET="viewer" HREF="{gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString}"><xsl:value-of select="gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString"/></A><BR/>
  </xsl:if>
</xsl:template>

<!-- Ruimtelijk schema -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialRepresentationType">
  <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue!=''">
    <B>Ruimtelijk schema: </B>
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='vector'">Vector</xsl:if>
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='grid'">Raster</xsl:if>
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='textTable'">TekstTabel</xsl:if>
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='tin'">TIN</xsl:if>
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='stereoModel'">Stereomodel</xsl:if>
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='video'">Video</xsl:if>
    <!-- Foutieve spelling in Nederlandse metadatastandaard voor geografie 1.1 -->
    <xsl:if test="gmd:MD_SpatialRepresentationTypeCode/@codeListValue='textTabel'">TekstTabel<a href="javascript:" title="Code foutief gespeld in XML" style="color: red; text-decoration: none">*</a></xsl:if>
    <BR/>
  </xsl:if>
</xsl:template>

<!-- Aanvullende informatie -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:supplementalInformation">
  <xsl:if test=".!=''">
    <xsl:for-each select=".">
      <DIV CLASS="pni">
        <xsl:call-template name="splitcomma_supplementalInformation">
          <xsl:with-param name="str" select="."/>
        </xsl:call-template>
      </DIV>
    </xsl:for-each>
  </xsl:if>
</xsl:template>
<xsl:template name="splitcomma_supplementalInformation">
  <xsl:param name="str" select="init"/>
  <!-- documentatie link -->
  <xsl:if test="not(contains($str, '|'))">
    <A TARGET="viewer" HREF="{$str}"><xsl:value-of select="$str"/></A><BR/>
  </xsl:if>
  <!-- loop again -->
  <xsl:if test="contains($str, '|')"><xsl:value-of select="substring-before($str, '|')"/>: </xsl:if>
  <xsl:if test="substring-after($str, '|')!=''">
    <xsl:call-template name="splitcomma_supplementalInformation">
      <xsl:with-param name="str" select="substring-after($str,'|')"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- Gerelateerde datasets -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:aggregationInfo">
  <xsl:if test="gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title!=''">
    <DIV CLASS="pni">
      <xsl:value-of select="gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title/gco:CharacterString"/>,
      <xsl:value-of select="gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date"/>
    </DIV>
  </xsl:if>
</xsl:template>

<!-- Taal van de bron -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:language">
  <xsl:if test="(gco:CharacterString!='') or (gmd:LanguageCode!='')">
    <BR/>
    <B>Taal van de bron: </B>
    <xsl:choose>
      <xsl:when test="contains(gco:CharacterString, 'dut')">Nederlands<BR/>
      </xsl:when>
      <xsl:when test="contains(gco:CharacterString, 'eng')">Engels<BR/>
      </xsl:when>
      <xsl:when test="contains(gco:CharacterString, 'en')">Engels<BR/>
      </xsl:when>
      <xsl:when test="contains(gco:CharacterString, 'deu')">Duits<BR/>
      </xsl:when>
      <xsl:when test="contains(gco:CharacterString, 'fre')">Frans<BR/>
      </xsl:when>
      <xsl:when test="contains(gco:CharacterString, 'spa')">Spaans<BR/>
      </xsl:when>
      <xsl:when test="contains(gco:CharacterString, 'fry')">Fries<BR/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="gco:CharacterString"/><BR/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<!-- Karakterset van de bron -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:characterSet">
  <xsl:if test="gmd:MD_CharacterSetCode/@codeListValue!=''">
    <B>Karakterset van de bron: </B><xsl:value-of select="gmd:MD_CharacterSetCode/@codeListValue"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Onderwerpen -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:topicCategory">
  <xsl:if test="gmd:MD_TopicCategoryCode!=''">
    <DIV CLASS="pni">
      <xsl:if test="gmd:MD_TopicCategoryCode='farming'">Landbouw en veeteelt</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='biota'">Flora en fauna</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='boundaries'">Grenzen</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='climatologyMeteorologyAtmosphere'">Klimatologie, meteorologie en atmosfeer</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='economy'">Economie</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='elevation'">Hoogte</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='environment'">Natuur en milieu</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='geoscientificInformation'">Geowetenschappelijke data</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='health'">Gezondheid</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='imageryBaseMapsEarthCover'">Referentiemateriaal aardbedekking</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='intelligenceMilitary'">Militair</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='inlandWaters'">Binnenwater</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='location'">Locatie</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='oceans'">Oceanen</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='planningCadastre'">Ruimtelijke ordening en kadaster</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='society'">Cultuur, maatschappij en demografie</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='structure'">(Civiele) structuren</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='transportation'">Transport en logistiek</xsl:if>
      <xsl:if test="gmd:MD_TopicCategoryCode='utilitiesCommunication'">Nutsvoorzieningen en communicatie</xsl:if>
    </DIV>
  </xsl:if>
</xsl:template>

<!-- Service identificatie -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification">
  <BR/>
  <B>Type service: </B>
  <xsl:value-of select="srv:serviceType/gco:LocalName"/>
  <BR/>
  <xsl:if test="srv:serviceTypeVersion/gco:CharacterString!=''">
    <B>Versie van de service specificatie: </B>
    <xsl:value-of select="srv:serviceTypeVersion/gco:CharacterString"/>
    <BR/>
  </xsl:if>
  <xsl:if test="count(srv:containsOperations/srv:SV_OperationMetadata)>0">
    <BR/>
    <B>Operaties:</B>
    <DIV CLASS="pni">
      <table>
      <tr>
        <td><b>Naam operatie</b></td>
        <td><b>DCP</b></td>
        <td><b>Netwerkadres</b></td>
      </tr>
      <xsl:apply-templates select="srv:containsOperations/srv:SV_OperationMetadata"/>
    </table>
    </DIV>
  </xsl:if>
  <xsl:if test="count(srv:coupledResource/srv:SV_CoupledResource)>0">
    <BR/>
    <B>Gekoppelde bronnen:</B>
    <DIV CLASS="pni">
      <table>
        <tr>
          <td><b>Naam operatie</b></td>
          <td><b>Identifier van de bron</b></td>
          <td><b>Naam binnen de service</b></td>
        </tr>
        <xsl:apply-templates select="srv:coupledResource/srv:SV_CoupledResource"/>
      </table>
    </DIV>
  </xsl:if>
  <xsl:if test="srv:couplingType/srv:SV_CouplingType/@codeListValue!=''">
    <BR/>
    <B>Type koppeling: </B>
    <xsl:value-of select="srv:couplingType/srv:SV_CouplingType/@codeListValue"/>
    <BR/>
  </xsl:if>
  <xsl:if test="count(srv:operatesOn)>0">
    <BR/>
    <B>Gerelateerde data:</B>
    <xsl:apply-templates select="srv:operatesOn"/>
  </xsl:if>
</xsl:template>

<!-- Operaties -->
<xsl:template match="gmd:identificationInfo/srv:SV_ServiceIdentification/srv:containsOperations/srv:SV_OperationMetadata">
  <tr>
    <td><xsl:value-of select="srv:operationName/gco:CharacterString"/></td>
    <td><xsl:value-of select="srv:DCP/srv:DCPList/@codeListValue"/></td>
    <td><xsl:value-of select="srv:connectPoint/gmd:CI_OnlineResource/gmd:linkage/gmd:URL"/></td>
  </tr>
</xsl:template>

  <!-- CoupledResource -->
  <xsl:template match="gmd:identificationInfo/srv:SV_ServiceIdentification/srv:coupledResource/srv:SV_CoupledResource">
    <tr>
      <td><xsl:value-of select="srv:operationName/gco:CharacterString"/></td>
      <td><xsl:value-of select="srv:identifier/gco:CharacterString"/></td>
      <td><xsl:value-of select="gco:ScopedName"/></td>
    </tr>
  </xsl:template>

  <!-- Gerelateerde data -->
<xsl:template match="gmd:identificationInfo/srv:SV_ServiceIdentification/srv:operatesOn">
  <xsl:if test="@uuidref!=''">
    <DIV CLASS="pni">
      Dataset <xsl:value-of select="@uuidref"/><xsl:if test="$GeoportalUrl!=''">
        <iframe src="{$GeoportalUrl}rest/find/document?searchText=datasetIdentifier:{@uuidref}&amp;f=html" style="vertical-align:middle;height:90px" scrolling="no" frameborder="0"></iframe>
      </xsl:if>
    </DIV>
  </xsl:if>
</xsl:template>

<!-- Contacten -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact">
  <BR/>
  <B>Contact <xsl:value-of select="gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString"/>:</B>
  <DIV CLASS="pni">
    <xsl:apply-templates select="gmd:CI_ResponsibleParty"/>
  </DIV>
</xsl:template>

<!-- Ruimtelijk referentie systeem -->
<xsl:template match="gmd:MD_Metadata/gmd:referenceSystemInfo[1]">
  <xsl:if test="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code!=''">
    <B>Code referentiesysteem: </B><xsl:value-of select="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:codeSpace!=''">
    <B>Verantwoordelijke organisatie voor namespace referentiesysteem: </B><xsl:value-of select="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:codeSpace"/><BR/>
  </xsl:if>
  <BR/>
</xsl:template>

<!-- Verticaal ruimtelijk referentie systeem -->
<xsl:template match="gmd:MD_Metadata/gmd:referenceSystemInfo[2]">
  <xsl:if test="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code!=''">
    <B>Code verticaal referentiesysteem: </B><xsl:value-of select="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:codeSpace!=''">
    <B>Verantwoordelijke organisatie voor namespace verticaal referentiesysteem: </B><xsl:value-of select="gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:codeSpace"/><BR/>
  </xsl:if>
</xsl:template>

<!-- Dekking -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent|gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:extent/gmd:EX_Extent">
  <xsl:if test="gmd:geographicElement!=''">
    <DIV STYLE="text-align:center; color:#2B669F">_________________</DIV>
    <BR/>
    <!-- Horizontale dekking -->
    <xsl:if test="gmd:geographicElement/gmd:EX_GeographicDescription/gmd:geographicIdentifier/gmd:MD_Identifier/gmd:code/gco:CharacterString!=''">
      <B>Beschrijving geografisch gebied: </B>
      <xsl:value-of select="gmd:geographicElement/gmd:EX_GeographicDescription/gmd:geographicIdentifier/gmd:MD_Identifier/gmd:code/gco:CharacterString"/>
      <BR/>
    </xsl:if>
    <xsl:if test="gmd:geographicElement/gmd:EX_GeographicBoundingBox[(gmd:westBoundLongitude!='') or (gmd:eastBoundLongitude!='') or (gmd:southBoundLatitude!='') or (gmd:northBoundLatitude!='')]">
      <DIV CLASS="ph1" onmouseover="doHilite()" onmouseout="doHilite()" onclick="hideShowGroup(this)">
        Omgrenzende rechthoek in decimale graden:
        <DIV CLASS="pni">
          <B>Minimum x-coördinaat: </B>
          <xsl:value-of select="gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude"/>
          <BR/>
          <B>Maximum x-coördinaat: </B>
          <xsl:value-of select="gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude"/>
          <BR/>
          <B>Minimum y-coördinaat: </B>
          <xsl:value-of select="gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude"/>
          <BR/>
          <B>Maximum y-coördinaat: </B>
          <xsl:value-of select="gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude"/>
          <BR/>
        </DIV>
      </DIV>
    </xsl:if>
  </xsl:if>
  <xsl:if test="gmd:verticalElement!=''">
    <DIV STYLE="text-align:center; color:#2B669F">_________________</DIV>
    <BR/>
    <!-- Verticale dekking -->
    <xsl:if test="gmd:verticalElement/gmd:EX_VerticalExtent/gmd:minimumValue/gco:Real!=''">
      <B>Minimum z-coördinaat: </B>
      <xsl:value-of select="gmd:verticalElement/gmd:EX_VerticalExtent/gmd:minimumValue"/>
      <BR/>
    </xsl:if>
    <xsl:if test="gmd:verticalElement/gmd:EX_VerticalExtent/gmd:maximumValue/gco:Real!=''">
      <B>Maximum z-coördinaat: </B>
      <xsl:value-of select="gmd:verticalElement/gmd:EX_VerticalExtent/gmd:maximumValue"/>
      <BR/>
    </xsl:if>
  </xsl:if>
  <xsl:if test="gmd:temporalElement!='' or gmd:description!=''">
    <DIV STYLE="text-align:center; color:#2B669F">_________________</DIV>
    <BR/>
    <!-- Temporele dekking -->
    <DIV CLASS="ph1" onmouseover="doHilite()" onmouseout="doHilite()" onclick="hideShowGroup(this)">
      Temporele dekking:
      <DIV CLASS="pni">
        <xsl:if test="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:begin/gml:TimeInstant/gml:timePosition!=''">
          <B>Van datum: </B>
          <xsl:value-of select="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:begin/gml:TimeInstant/gml:timePosition"/>
          <BR/>
        </xsl:if>
        <xsl:if test="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:end/gml:TimeInstant/gml:timePosition!=''">
          <B>Tot datum: </B>
          <xsl:value-of select="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:end/gml:TimeInstant/gml:timePosition"/>
          <BR/>
        </xsl:if>
        <xsl:if test="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition!=''">
          <B>Van datum: </B>
          <xsl:value-of select="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition"/>
          <BR/>
        </xsl:if>
        <xsl:if test="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition!=''">
          <B>Tot datum: </B>
          <xsl:value-of select="gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition"/>
          <BR/>
        </xsl:if>
        <xsl:if test="gmd:description!=''">
          <B>Beschrijving temporele dekking: </B>
          <xsl:value-of select="gmd:description"/>
          <BR/>
        </xsl:if>
      </DIV>
    </DIV>
  </xsl:if>
</xsl:template>

<!-- Kwaliteit -->
<xsl:template match="gmd:MD_Metadata/gmd:dataQualityInfo">
  <BR/>
  <xsl:if test="gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString!=''">
    <B>Algemene beschrijving herkomst: </B><xsl:value-of select="gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:DQ_DataQuality/gmd:report/gmd:DQ_AbsoluteExternalPositionalAccuracy/gmd:result/gmd:DQ_QuantitativeResult/gmd:value!=''">
    <B>Geometrische nauwkeurigheid: </B><xsl:value-of select="gmd:DQ_DataQuality/gmd:report/gmd:DQ_AbsoluteExternalPositionalAccuracy/gmd:result/gmd:DQ_QuantitativeResult/gmd:value"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:DQ_DataQuality/gmd:report/gmd:DQ_CompletenessOmission/gmd:result/gmd:DQ_QuantitativeResult/gmd:value!=''">
    <B>Volledigheid: </B><xsl:value-of select="gmd:DQ_DataQuality/gmd:report/gmd:DQ_CompletenessOmission/gmd:result/gmd:DQ_QuantitativeResult/gmd:value"/><BR/>
  </xsl:if>
  <BR/>
  <xsl:if test="(count(gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency)!=0)">
    <DIV CLASS="ph1" onmouseover="doHilite()" onmouseout="doHilite()" onclick="hideShowGroup(this)">
      Specificaties:
      <DIV CLASS="pni">
        <xsl:for-each select="gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency">
          <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:title/gco:CharacterString!=''">
            <B><xsl:text>Titel: </xsl:text></B>
            <xsl:value-of select="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:title/gco:CharacterString"/>
            <BR/>
          </xsl:if>
          <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date">
            <B><xsl:text>Datum</xsl:text>
            <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='creation'">
              <xsl:text> voltooiing</xsl:text>
            </xsl:if>
            <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='publication'">
              <xsl:text> publicatie</xsl:text>
            </xsl:if>
            <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='revision'">
              <xsl:text> revisie</xsl:text>
            </xsl:if>
            <xsl:text>: </xsl:text></B>
            <xsl:value-of select="gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date"/>
            <BR/>
          </xsl:if>
          <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:pass/gco:Boolean!=''">
            <B><xsl:text>Conformiteitsindicatie: </xsl:text></B>
            <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:pass/gco:Boolean='true'">
              <xsl:text>conform</xsl:text>
            </xsl:if>
            <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:pass/gco:Boolean='false'">
              <xsl:text>niet conform</xsl:text>
            </xsl:if>
            <BR/>
          </xsl:if>
          <xsl:if test="gmd:result/gmd:DQ_ConformanceResult/gmd:explanation/gco:CharacterString!=''">
            <B><xsl:text>Verklaring: </xsl:text></B>
            <xsl:value-of select="gmd:result/gmd:DQ_ConformanceResult/gmd:explanation/gco:CharacterString"/>
            <BR/>
          </xsl:if>
          <BR/>
        </xsl:for-each>
      </DIV>
    </DIV>
  </xsl:if>
  <xsl:if test="(count(gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:processStep)!=0)">
    <DIV CLASS="ph1" onmouseover="doHilite()" onmouseout="doHilite()" onclick="hideShowGroup(this)">Uitgevoerde bewerkingen:
      <DIV CLASS="pni">
        <xsl:for-each select="gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:processStep">
          <xsl:if test="gmd:LI_ProcessStep/gmd:description/gco:CharacterString!=''">
            <B><xsl:text>Beschrijving: </xsl:text></B><xsl:value-of select="gmd:LI_ProcessStep/gmd:description/gco:CharacterString"/><BR/>
          </xsl:if>
          <xsl:variable name="datum" select="gmd:LI_ProcessStep/gmd:dateTime"/>
          <xsl:if test="$datum!=''">
            <B><xsl:text>Datum bewerking: </xsl:text></B><xsl:value-of select="substring-before($datum, 'T')"/><BR/>
          </xsl:if>
          <xsl:if test="gmd:LI_ProcessStep/gmd:processor/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString!=''">
            <B><xsl:text>Bewerkende organisatie: </xsl:text></B><xsl:value-of select="gmd:LI_ProcessStep/gmd:processor/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString"/><BR/>
          </xsl:if>
          <BR/>
        </xsl:for-each>
      </DIV>
    </DIV>
  </xsl:if>
  <xsl:if test="(count(gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:source)!=0)">
    <DIV CLASS="ph1" onmouseover="doHilite()" onmouseout="doHilite()" onclick="hideShowGroup(this)">Gebruikte bronbestanden:
      <DIV CLASS="pni">
        <xsl:for-each select="gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:source">
          <xsl:if test="gmd:LI_Source/gmd:description/gco:CharacterString!=''">
            <B>Beschrijving: </B><xsl:value-of select="gmd:LI_Source/gmd:description/gco:CharacterString"/><BR/>
          </xsl:if>
          <xsl:if test="gmd:LI_Source/gmd:sourceStep/gmd:LI_ProcessStep/gmd:description/gco:CharacterString!=''">
            <B>Inwinningsmethode: </B><xsl:value-of select="gmd:LI_Source/gmd:sourceStep/gmd:LI_ProcessStep/gmd:description/gco:CharacterString"/><BR/>
          </xsl:if>
          <xsl:variable name="datum" select="gmd:LI_Source/gmd:sourceStep/gmd:LI_ProcessStep/gmd:dateTime"/>
          <xsl:if test="$datum!=''">
            <xsl:choose>
              <xsl:when test="contains($datum, 'T')">
                <B>Datum inwinning: </B><xsl:value-of select="substring-before($datum, 'T')"/><BR/>
              </xsl:when>
              <xsl:otherwise>
                <B>Datum inwinning: </B><xsl:value-of select="gmd:LI_Source/gmd:sourceStep/gmd:LI_ProcessStep/gmd:dateTime"/><BR/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:if test="gmd:LI_Source/gmd:sourceStep/gmd:LI_ProcessStep/gmd:processor/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString!=''">
            <B>Inwinnende organisatie: </B><xsl:value-of select="gmd:LI_Source/gmd:sourceStep/gmd:LI_ProcessStep/gmd:processor/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString"/><BR/>
          </xsl:if>
          <BR/>
        </xsl:for-each>
      </DIV>
    </DIV>
  </xsl:if>
</xsl:template>

<!-- Datamodel -->
<xsl:template match="gmd:MD_Metadata/gmd:applicationSchemaInfo">
  <xsl:if test="gmd:MD_ApplicationSchemaInformation/gmd:name/gmd:CI_Citation/gmd:title/gco:CharacterString!=''">
    <BR/>
    <B>Naam applicatieschema: </B><xsl:value-of select="gmd:MD_ApplicationSchemaInformation/gmd:name/gmd:CI_Citation/gmd:title"/><BR/>
  </xsl:if>
  <xsl:if test="gmd:MD_ApplicationSchemaInformation/gmd:name/gmd:CI_Citation/gmd:date!=''">
    <xsl:for-each select="gmd:MD_ApplicationSchemaInformation/gmd:name/gmd:CI_Citation/gmd:date">
      <xsl:if test="gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='creation'">
        <B>Datum voltooiing: </B><xsl:value-of select="gmd:CI_Date/gmd:date/gco:Date"/><BR/>
      </xsl:if>
      <xsl:if test="gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='publication'">
        <B>Datum publicatie: </B><xsl:value-of select="gmd:CI_Date/gmd:date/gco:Date"/><BR/>
      </xsl:if>
      <xsl:if test="gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue='revision'">
        <B>Datum laatste wijziging: </B><xsl:value-of select="gmd:CI_Date/gmd:date/gco:Date"/><BR/>
      </xsl:if>
    </xsl:for-each>
    <xsl:if test="gmd:MD_ApplicationSchemaInformation/gmd:schemaLanguage!=''">
      <B>Taal van applicatieschema: </B><xsl:value-of select="gmd:MD_ApplicationSchemaInformation/gmd:schemaLanguage"/><BR/>
    </xsl:if>
    <xsl:if test="gmd:MD_ApplicationSchemaInformation/gmd:constraintLanguage!=''">
      <B>Constraintlanguage van applicatieschema: </B><xsl:value-of select="gmd:MD_ApplicationSchemaInformation/gmd:constraintLanguage"/><BR/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<!-- attribuutinformatie -->
<xsl:template match="/metadata/FC_FeatureCatalogue">
  <BR/>
  <xsl:for-each select="/metadata/FC_FeatureCatalogue/featureType">
    <xsl:if test="((name!='') or (definition!='') or (featureAttribute/name!='') or (featureAttribute/definition!=''))">
      <DIV CLASS="pn">
        <xsl:if test="name!=''">
          <B>Object naam: </B><xsl:value-of select="name"/><BR/>
        </xsl:if>
        <xsl:if test="definition!=''">
          <I>Object definitie: </I><xsl:value-of select="definition"/><BR/>
        </xsl:if>
        <xsl:if test="featureAttribute/name!=''">
          <DIV CLASS="pn">
            <DIV CLASS="ph1" onmouseover="doHilite()" onmouseout="doHilite()" onclick="hideShowGroup(this)">Attributen
              <DIV CLASS="pni">
                <xsl:for-each select="featureAttribute">
                  <xsl:if test="name!=''">
                    <B><xsl:value-of select="name"/></B>
                  </xsl:if>
                  <xsl:if test="definition!=''">
                    <B>: </B><I><xsl:value-of select="definition"/></I>
                  </xsl:if>
                  <BR/>
                </xsl:for-each>
              </DIV>
            </DIV>
          </DIV>
        </xsl:if>
        <BR/>
      </DIV>
    </xsl:if>
  </xsl:for-each>
</xsl:template>

<!-- Distribuerende organisatie -->
<xsl:template match="gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact[gmd:CI_ResponsibleParty]">
  <BR/>
  <B>Distributeur:</B>
  <DIV CLASS="pni">
    <xsl:if test="gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString!=''">
      <DIV CLASS="pn"><B>Naam organisatie: </B><xsl:value-of select="gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString"/><BR/></DIV>
    </xsl:if>
    <xsl:apply-templates select="gmd:CI_ResponsibleParty"/>
  </DIV>
</xsl:template>

<!-- Distributieformaten -->
<xsl:template match="gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution">
  <BR/>
  <xsl:if test="gmd:distributionFormat/gmd:MD_Format/gmd:name/gco:CharacterString!=''">
<!--
    <B>Naam en versie van distributieformaten: </B><BR/>
    <xsl:for-each select="gmd:distributionFormat">
      <DIV CLASS="pni">
        <xsl:if test="gmd:MD_Format/gmd:name/gco:CharacterString!=''">
          <xsl:value-of select="gmd:MD_Format/gmd:name/gco:CharacterString"/>
        </xsl:if>
        <xsl:if test="gmd:MD_Format/gmd:version/gco:CharacterString!=''">, versie <xsl:value-of select="gmd:MD_Format/gmd:version/gco:CharacterString"/>
        <BR/>
        </xsl:if>
      </DIV>
    </xsl:for-each>
    <BR/>
-->	  
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions">
  <xsl:if test="gmd:unitsOfDistribution/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Leverings-/gebruikseenheid: </B><xsl:value-of select="gmd:unitsOfDistribution/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
<!--
  <xsl:if test="gmd:onLine/gmd:CI_OnlineResource/gmd:linkage/gmd:URL!=''">
    <DIV CLASS="pn"><B>Locatie dataset: </B><xsl:value-of select="gmd:onLine/gmd:CI_OnlineResource/gmd:linkage/gmd:URL"/><BR/></DIV>
  </xsl:if>
-->
  <xsl:if test="gmd:offLine/gmd:MD_Medium/gmd:name/gmd:MD_MediumNameCode/@codeListValue!=''">
    <DIV CLASS="pn"><B>Naam medium: </B>
      <xsl:variable name="medium" select="gmd:offLine/gmd:MD_Medium/gmd:name/gmd:MD_MediumNameCode/@codeListValue"/>
      <xsl:if test="$medium='dvd'">dvd</xsl:if>
      <xsl:if test="$medium='cdRom'">cd-rom</xsl:if>
      <xsl:if test="$medium='onLine'">Via internet</xsl:if>
      <xsl:if test="$medium='hardcopy'">Afdruk</xsl:if>
      <BR/>
    </DIV>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributionOrderProcess">
  <xsl:if test="gmd:MD_StandardOrderProcess/gmd:fees/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Prijsinformatie: </B><xsl:value-of select="gmd:MD_StandardOrderProcess/gmd:fees"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:MD_StandardOrderProcess/gmd:orderingInstructions/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Orderprocedure: </B><xsl:value-of select="gmd:MD_StandardOrderProcess/gmd:orderingInstructions"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:MD_StandardOrderProcess/gmd:turnaround/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Doorlooptijd orderprocedure: </B><xsl:value-of select="gmd:MD_StandardOrderProcess/gmd:turnaround"/><BR/></DIV>
  </xsl:if>
</xsl:template>

<!-- Gebruiksbeperkingen -->
<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_Constraints">
  <xsl:if test="gmd:useLimitation/gco:CharacterString!=''">
    <DIV CLASS="pni">
      <xsl:value-of select="gmd:useLimitation/gco:CharacterString"/><BR/>
    </DIV>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:accessConstraints">
  <xsl:if test="gmd:MD_RestrictionCode/@codeListValue!=''">
    <DIV CLASS="pni">
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='copyright'">Copyright<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='patent'">Patent<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='patentPending'">Patent in wording<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='trademark'">Merknaam<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='license'">Licentie<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='intellectualPropertyRights'">Intellectueel eigendom<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='restricted'">Niet toegankelijk<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='otherRestrictions'">Overig<BR/></xsl:if>
    </DIV>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:useConstraints">
  <xsl:if test="gmd:MD_RestrictionCode/@codeListValue!=''">
    <DIV CLASS="pni">
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='copyright'">Copyright<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='patent'">Patent<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='patentPending'">Patent in wording<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='trademark'">Merknaam<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='license'">Licentie<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='intellectualPropertyRights'">Intellectueel eigendom<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='restricted'">Niet toegankelijk<BR/></xsl:if>
      <xsl:if test="gmd:MD_RestrictionCode/@codeListValue='otherRestrictions'">Overig<BR/></xsl:if>
    </DIV>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:otherConstraints">
  <B>Overige beperkingen: </B>
  <xsl:if test="(.!='') and (gco:CharacterString!='no limitation on public access')">
    <xsl:value-of select="gco:CharacterString"/><BR/>
  </xsl:if>
</xsl:template>

<xsl:template match="gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/gmd:MD_SecurityConstraints">
  <xsl:if test="gmd:classification/gmd:MD_ClassificationCode/@codeListValue!=''">
    <B>Veiligheidsrestricties: </B>
    <xsl:if test="gmd:classification/gmd:MD_ClassificationCode/@codeListValue='unclassified'">Vrij toegankelijk<BR/></xsl:if>
    <xsl:if test="gmd:classification/gmd:MD_ClassificationCode/@codeListValue='restricted'">Niet toegankelijk<BR/></xsl:if>
    <xsl:if test="gmd:classification/gmd:MD_ClassificationCode/@codeListValue='confidential'">Vertrouwelijk<BR/></xsl:if>
    <xsl:if test="gmd:classification/gmd:MD_ClassificationCode/@codeListValue='secret'">Geheim<BR/></xsl:if>
    <xsl:if test="gmd:classification/gmd:MD_ClassificationCode/@codeListValue='topSecret'">Topgeheim<BR/></xsl:if>
  </xsl:if>
  <xsl:if test="gmd:userNote/gco:CharacterString!=''">
    <B>Toelichting veiligheidsrestricties: </B><xsl:value-of select="gmd:userNote/gco:CharacterString"/><BR/>
    <BR/>
  </xsl:if>
</xsl:template>

<!-- CI_ResponsibleParty -->
<xsl:template match="gmd:CI_ResponsibleParty">
  <xsl:variable name="Role" select="gmd:role/gmd:CI_RoleCode/@codeListValue"/>
  <xsl:if test="$Role!=''">
    <DIV CLASS="pn">
      <B>Rol organisatie: </B>
      <xsl:if test="$Role='resourceProvider'">Verstrekker</xsl:if>
      <xsl:if test="$Role='custodian'">Beheerder</xsl:if>
      <xsl:if test="$Role='owner'">Eigenaar</xsl:if>
      <xsl:if test="$Role='user'">Gebruiker</xsl:if>
      <xsl:if test="$Role='distributor'">Distributeur</xsl:if>
      <xsl:if test="$Role='originator'">Maker</xsl:if>
      <xsl:if test="$Role='pointOfContact'">Contactpunt</xsl:if>
      <xsl:if test="$Role='principalInvestigator'">Inwinner</xsl:if>
      <xsl:if test="$Role='processor'">Bewerker</xsl:if>
      <xsl:if test="$Role='publisher'">Uitgever</xsl:if>
      <xsl:if test="$Role='author'">Auteur</xsl:if>
      <!-- Foutieve spelling in Nederlandse metadatastandaard voor geografie 1.1 -->
      <xsl:if test="$Role='resourceprovider'">Verstrekker<a href="javascript:" title="Code foutief gespeld in XML" style="color: red; text-decoration: none">*</a></xsl:if>
      <BR/>
    </DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL!=''">
    <DIV CLASS="pn"><B>Website organisatie: </B><A TARGET="viewer" HREF="{gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL}"><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL"/></A><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:individualName/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Naam contactpersoon: </B><xsl:value-of select="gmd:individualName/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:positionName/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Rol contactpersoon: </B><xsl:value-of select="gmd:positionName/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>E-mail: </B><A HREF="mailto:{gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString}"><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString"/></A><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:deliveryPoint/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Adres: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:deliveryPoint/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:postalCode/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Postcode: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:postalCode/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:city/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Plaats: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:city/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:administrativeArea/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Provincie: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:administrativeArea/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:country/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Land: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:country/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:phone/gmd:CI_Telephone/gmd:voice/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Telefoonnummer: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:phone/gmd:CI_Telephone/gmd:voice/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
  <xsl:if test="gmd:contactInfo/gmd:CI_Contact/gmd:phone/gmd:CI_Telephone/gmd:facsimile/gco:CharacterString!=''">
    <DIV CLASS="pn"><B>Faxnummer: </B><xsl:value-of select="gmd:contactInfo/gmd:CI_Contact/gmd:phone/gmd:CI_Telephone/gmd:facsimile/gco:CharacterString"/><BR/></DIV>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>