name := """publisher-web"""

version := (xml.XML.loadFile("pom.xml") \\ "project" \ "parent" \ "version" ).map (_.text).head

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  "org.webjars" %% "webjars-play" % "2.3.0",  
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.webjars" % "dojo" % "1.10.0",
  "nl.idgis.publisher" % "publisher-domain" % (xml.XML.loadFile("pom.xml") \\ "project" \ "parent" \ "version" ).map (_.text).head,
  "com.typesafe.akka" %% "akka-remote" % "2.3.3",
  "org.pegdown" % "pegdown" % "1.5.0",
  "org.apache.zookeeper" % "zookeeper" % "3.4.5"
)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

LessKeys.compress := false

// pipelineStages := Seq(rjs)

resolvers += Resolver.mavenLocal

resolvers += "idgis-public" at "http://nexus.idgis.eu/content/groups/public/"

resolvers += "idgis-thirdparty" at "http://nexus.idgis.eu/content/repositories/thirdparty/"

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "planoview"

buildInfoPackage := "publisher"