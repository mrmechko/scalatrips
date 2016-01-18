package strips.read


import supplies.sexpr._

import utest._


object StripsOntReaderTests extends TestSuite {
  val tests = TestSuite{
    'load {
      "as slist" - assert(slist(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n")).map(_.size) == Some(23))
      "as file " - assert(supplies.sexpr.parser.parseAll.parse(
        scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n")
      ).get.value.size == 23)
    }
  }
}
