name := """publisher-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.webjars" %% "webjars-play" % "2.3.0",  
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.webjars" % "dojo" % "1.10.0"
)

includeFilter in (Assets, LessKeys.less) := "*.less"