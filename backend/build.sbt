name := "Test"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-google-cloud-pub-sub" % "2.0.0-M3"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.2"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.4.1"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.1"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.22.0"


libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.2"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2"
libraryDependencies += "com.microsoft.sqlserver" % "mssql-jdbc" % "8.2.1.jre8"





lazy val runProdNal = taskKey[Unit]("Run Prod Nal")

fork in runProdNal := true
javaOptions in runProdNal += "-Dconfig.resource=prod_nal.conf"

fullRunTask(runProdNal, Compile, "IntelligentEmergencyDepartment.Main")

