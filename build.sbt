name := "loganalysis"

version := "1.0"

lazy val `loganalysis` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test )

libraryDependencies ++= Seq(
  "com.databricks" % "spark-csv_2.11" % "1.4.0",
  "org.apache.spark" % "spark-core_2.11" % "1.6.1",
  "org.apache.spark" % "spark-sql_2.11" % "1.6.1",
  "org.scalacheck" % "scalacheck_2.11" % "1.13.1"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

dependencyOverrides ++= Set(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
)

//scalacOptions in ThisBuild += "-Xlog-implicits"

javaOptions in run += "-Xms512M -Xmx4096M"
