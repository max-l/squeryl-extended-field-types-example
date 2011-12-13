
name := "squeryl-extended-field-types-example"

organization := "org.squeryl"

version := "0.1"


scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "org.squeryl" %% "squeryl" % "0.9.5-extended-types-poc",
  "joda-time" % "joda-time" % "1.6" % "provided",
  "com.h2database" % "h2" % "1.2.127"
)
