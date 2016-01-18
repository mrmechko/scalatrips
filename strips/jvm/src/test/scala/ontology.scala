package strips.read


import supplies.sexpr._

import utest._


object StripsOntReaderTests extends TestSuite {
  val tests = TestSuite{
    'load {
      "find file" - getClass.getResourceAsStream("/test.lisp")
      'load {
        * - slist(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n")).map(_.size)
        * - supplies.sexpr.parser.parseAll.parse(
          scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n")
        ).get.value.size
      }
    }
  }
}
