import mill._
import mill.scalalib._

object core extends ScalaModule with scalafmt.ScalafmtModule {
  def scalaVersion = "3.8.2"

  object test extends ScalaTests with TestModule.ZioTest
}
