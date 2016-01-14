package strips.read


import supplies.sexpr._

import utest._


object StripsOntReaderTests extends TestSuite {
  val tests = TestSuite{
    'testTest - assert(true)

    'load {
      slist(io.Source.fromURL(getClass.getResource("/test-ont.lisp")).toList.mkString)
    }
  }
}
