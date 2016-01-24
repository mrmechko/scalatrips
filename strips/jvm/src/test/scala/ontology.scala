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
        val file = sl.map(_.tail.map(e => (new OntDefineTypeReader(e.asInstanceOf[ast.Lst].value))())).get.collect{case Some(a) => a}
        "convert" - assert(file.size == 22)
      }

      'sem {
        val oneofreader = new SemTypeReader(
          Seq(ast.Lst(ast.Var("?"), ast.Var("rst"), ast.Var("F::One"), ast.Var("F::Two"))), "test"
        )
        val oneofinherits = oneofreader().map(_.inherits)
        val shouldBe = Some(List(SemInherits("F::One"), SemInherits("F::Two")))
        * - assert( oneofinherits == shouldBe )
      }
    }
  }
}
