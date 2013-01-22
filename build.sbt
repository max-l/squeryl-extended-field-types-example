name := "squeryl-extended-field-types-example"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

organization := "org.squeryl"

version := "0.1"


scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "org.squeryl" %% "squeryl" % "0.9.6-SNAPSHOT",
  "joda-time" % "joda-time" % "1.6",
  "com.h2database" % "h2" % "1.2.127"
)
