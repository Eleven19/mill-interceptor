import zio.test.*

object MainSpec extends ZIOSpecDefault:
  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("MainSpec")(
      test("publish fixture test surface is available") {
        assertTrue(1 + 1 == 2)
      }
    )
