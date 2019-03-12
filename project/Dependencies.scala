import sbt._

object Dependencies {

  private object Versions {
    lazy val monadicFor = "0.2.4"
    lazy val zio        = "1.0-RC1"
  }

  lazy val monadicFor = "com.olegpy" %% "better-monadic-for" % Versions.monadicFor
  lazy val zio        = "org.scalaz" %% "scalaz-zio"         % Versions.zio
}
