package strips.read


import supplies.sexpr._

import utest._


object StripsOntReaderTests extends TestSuite {
  val tests = TestSuite{
    'load {
      val sl = slist(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n"))
      "as slist" - assert(sl.map(_.size) == Some(23))
      "as file " - assert(supplies.sexpr.parser.parseAll.parse(
        scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test.lisp")).getLines.mkString("\n")
      ).get.value.size == 23)

      'onttype {
        "convert" - sl.map(_.tail.map(e => (new OntDefineTypeReader(e.asInstanceOf[ast.Lst].value))()))
      }
    }
  }
}
