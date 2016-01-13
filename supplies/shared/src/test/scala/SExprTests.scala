package supplies


import sexpr._
import fastparse.all._

import utest._

object SexprTests extends TestSuite {
  val tests = TestSuite{
    'pass {
      def test(p: P[_], s: String) = p.parse(s) match{
        case Parsed.Success(v, i) =>
        val expectedIndex = s.length
        assert(i == expectedIndex)
        case f: Parsed.Failure => throw new Exception(f.extra.traced.fullStack.mkString("\n"))
      }

      def failTest(p : P[_], s: String) = p.parse(s) match {
        case Parsed.Success(v, i) => assert(false)
        case f : Parsed.Failure => assert(true)
      }

      def testValue[T](p : P[_], s : String, res : T) = p.parse(s) match {
        case Parsed.Success(v, i) => {
          assert(v == res)
        } case f: Parsed.Failure => throw new Exception(f.extra.traced.fullStack.mkString("\n"))
      }

      'atoms {
        'number {
          "positive"  - testValue(parser.number, "1", ast.Num(1))
          "decimal"   - testValue(parser.number, "1.2", ast.Num(1.2))
          "negative"  - testValue(parser.number, "-1", ast.Num(-1))

          'notNumbers {
            "string"    - failTest(parser.number, "\"1a\"")
            "symbol"    - failTest(parser.number, "'ejecta")
            "variable"  - failTest(parser.number, "timenow1")
            "lst"       - failTest(parser.number, "(2)")
          }
        }
        'string {
          "a string"      - testValue(parser.string, "\"i am a cow lol omfg\"", ast.Str("i am a cow lol omfg"))
          "empty"         - testValue(parser.string, "\"\"", ast.Str(""))
          "with escapes"  - testValue(parser.string, "\"escapee: \\\"\"", ast.Str("escapee: \\\""))
          "multiline"     - testValue(parser.string, "\"linebreak: \nbroke\"", ast.Str("linebreak: \nbroke"))
          'notString {
            "number"    - failTest(parser.string, "2.2")
            "symbol"    - failTest(parser.string, "'some")
            "variable"  - failTest(parser.string, "renegades")
            "lst"       - failTest(parser.string, "(\"hey, hey hey hey\")")
          }
        }
        * - test(parser.symbol, "'symbol")
        'variable{
          "just a var"    - testValue(parser.variable, "variable", ast.Var("variable"))
          "with numbers"  - testValue(parser.variable, "variable2", ast.Var("variable2"))
          "with scope"    - testValue(parser.variable, "hey::renegade", ast.Var("hey::renegade"))
          "multiline"     - testValue(parser.variable, "multiline", ast.Var("multiline"))
          "long name"     - testValue(parser.variable, "longvariablename", ast.Var("longvariablename"))
          "mstart"        - testValue(parser.variable, "ma", ast.Var("ma"))
          'notVar {
            "actually a key"    - failTest(parser.variable, ":key")
            "actually a lst"    - failTest(parser.variable, "(variable)")
            "actually a num"    - failTest(parser.variable, "2.2")
            "actually a string" - failTest(parser.variable, "\"Gimme just one chance now\"")
            "actually a symbol" - failTest(parser.variable, "'inside-out-come-home-lookin-like-your-head-fell-off")
          }
        }
      }

      'exprs {
        'lst {
            "nil"       - testValue(parser.lst, "()", ast.Lst())
            "single"    - testValue(parser.lst, "(1)", ast.Lst(ast.Num(1)))
            "multiple"  - testValue(parser.lst, "(trustful \"hands\")", ast.Lst(ast.Var("trustful"), ast.Str("hands")))
            "nested"    - testValue(parser.lst, """(1 2 "omg" ("wtf" 42))""",
              ast.Lst(
                ast.Num(1), ast.Num(2), ast.Str("omg"), ast.Lst(ast.Str("wtf"), ast.Num(42))
              )
            )
        }
      }

      'parser {
        'atoms {
          "number"    - testValue(parser.parse, "1", ast.Num(1))
          "string"    - testValue(parser.parse, "\"string\"", ast.Str("string"))
          "symbol"    - testValue(parser.parse, "'symbol", ast.Sym("symbol"))
          "variable"  - testValue(parser.parse, "variable", ast.Var("variable"))
        }

        'sexp {
          "number"    - testValue(parser.parse, "(fn 1)", ast.Lst(ast.Var("fn"), ast.Num(1)))
          "string"    - testValue(parser.parse, "(fn \"string\")", ast.Lst(ast.Var("fn"), ast.Str("string")))
          "symbol"    - testValue(parser.parse, "(fn 'symbol)", ast.Lst(ast.Var("fn"),ast.Sym("symbol")))
          "variable"  - testValue(parser.parse, "(fn variable)", ast.Lst(ast.Var("fn"),ast.Var("variable")))
        }

        'weird { //Tests to find and fix the strange variable-name error
          "working"  - test(parser.parse, "(fn variable)")
          "failing"  - test(parser.parse, "(some multiline)")//, ast.Lst(ast.Var("some"), ast.Var("multiline"), ast.Var("expression")))
          "combo-1"  - test(parser.parse, "(fn multiline)")
          "combo-2"  - test(parser.parse, "(some variable)")
          "combo-3"  - test(parser.parse, "(some variable multiline)")
        }

        'multiline {
          "singleLine"  - test(parser.parse, "(some multiline)")//, ast.Lst(ast.Var("some"), ast.Var("multiline"), ast.Var("expression")))

          "simple"  - testValue(parser.parse,
            """
            |(some
            |  multiline
            |expression)""".stripMargin, ast.Lst(ast.Var("some"), ast.Var("multiline"), ast.Var("expression")))
        }

        'inlinecomments {
          'atoms {
            "number"    - testValue(parser.parse, "1 ;comment", ast.Num(1))
            "string"    - testValue(parser.parse, "\"string\";comment", ast.Str("string"))
            "strcomment"- testValue(parser.parse, "\"string;comment\"", ast.Str("string;comment"))
            "symbol"    - testValue(parser.parse, "'symbol ;comment", ast.Sym("symbol"))
            "variable"  - testValue(parser.parse, "variable ;comment", ast.Var("variable"))
          }
          "line end comments" - testValue(parser.parse, "(fn variable) ;a comment", ast.Lst(ast.Var("fn"), ast.Var("variable")))
          "interspersed ml"   - testValue(parser.parse,
            """
            |(some ;;comment
            |  multiline ;;comments
            |expression)""".stripMargin, ast.Lst(ast.Var("some"), ast.Var("multiline"), ast.Var("expression")))
        }

        'blockcomments {
          * - assert(true)
        }
      }

      'unpack {
        'str {
          * - assert(unpack.string(ast.Str("some")) == Some("some"))
          * - assert(unpack.string(ast.Var("some")) == None)
        }
        'dsl {
          * - assert(unpack.string(ast.Str("some")) == Some("some"))
          * - assert(unpack.double(ast.Str("some")) == None)
          * - assert(unpack.double(ast.Num(1)) == Some(1.0))
          * - println(unpack.list(ast.Lst(ast.Str("some"), ast.Str("thing"))))
          val a = unpack.list(ast.Lst(ast.Str("some"), ast.Str("thing")))
          val b = Some(List(ast.Str("some"), ast.Str("thing")))
          * - assert(a == b)
          * - assert(unpack.listString(ast.Lst(ast.Str("some"), ast.Str("thing"))) == Some(List("some", "thing")))
        }
      }

    }
  }
}
