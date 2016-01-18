package supplies


import sexpr._
import fastparse.all._

import utest._

object JVMLoadFileTest extends TestSuite {
  val tests = TestSuite{
    'pass {
      val text = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n")
      val loaded = supplies.sexpr.parser.parseAll.parse(
        text
      ).get

      val size = loaded.value.size
      "loaded" - loaded.value.last
      "index" - loaded.index
      //"whatelse" - text.drop(loaded.index)
      "all" - assert(size == 23)
    }
  }
}
