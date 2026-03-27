package build

import mill.*
import mill.scalalib.*

trait DocSiteSupport extends ScalaModule {

  def docSiteRoot: T[PathRef] = Task.Source(Task.ctx().workspace / "docs")

  override def docResources = Task {
    val root = docSiteRoot().path
    val filtered = Task.dest / "filtered"
    os.makeDir.all(filtered)

    val entries = Seq("_docs", "_blog", "_assets", "sidebar.yml")
    for name <- entries do
      val src = root / name
      if os.exists(src) then os.copy(src, filtered / name, createFolders = true)

    Seq(PathRef(filtered))
  }

  override def scalaDocOptions = Task {
    Seq(
      "-project",
      "mill-interceptor",
      "-project-version",
      sys.env.getOrElse("MILLI_PUBLISH_VERSION", "0.0.0-SNAPSHOT"),
      "-source-links:github://Eleven19/mill-interceptor/main",
      "-social-links:github::https://github.com/Eleven19/mill-interceptor",
      "-no-link-warnings"
    )
  }

  def docSiteServe(port: Int = 8080) = Task.Command {
    val site = scalaDocGenerated()
    Task.log.info(s"Serving at http://localhost:$port")
    Task.log.info("Press Ctrl+C to stop.")
    os.proc("python3", "-m", "http.server", port.toString)
      .call(cwd = site.path, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
  }
}
