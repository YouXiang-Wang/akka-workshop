import sbt._

logLevel := Level.Warn

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"


addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.6")

// addSbtPlugin("io.kamon" % "sbt-kanela-runner" % "2.0.9")