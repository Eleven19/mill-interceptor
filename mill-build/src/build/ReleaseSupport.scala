package build

import mill.*

trait ReleaseSupport extends mill.Module:
  def releaseTargets = Task {
    Seq(
      "x86_64-unknown-linux-gnu",
      "aarch64-unknown-linux-gnu",
      "x86_64-apple-darwin",
      "aarch64-apple-darwin",
      "x86_64-pc-windows-msvc"
    )
  }
