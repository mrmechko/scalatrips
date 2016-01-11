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
        'Kv {
          "nil"       - testValue(parser.kv, ":key ()", ast.Kv(("key", ast.Lst())))
          "lst"       - testValue(parser.kv, ":key (variable 1)", ast.Kv(("key", ast.Lst(ast.Var("variable"), ast.Num(1)))))
          "symbol"    - testValue(parser.kv, ":key 'symbol", ast.Kv(("key", ast.Sym("symbol"))))//Key-symbol fails
          "int"       - testValue(parser.kv, ":key 2", ast.Kv(("key", ast.Num(2))))
        }
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
          "number"    - testValue(parser.sExpr, "1", ast.Num(1))
          "string"    - testValue(parser.sExpr, "\"string\"", ast.Str("string"))
          "symbol"    - testValue(parser.sExpr, "'symbol", ast.Sym("symbol"))
          "variable"  - testValue(parser.sExpr, "variable", ast.Var("variable"))
        }

        'sexp {
          "number"    - testValue(parser.sExpr, "(fn 1)", ast.Lst(ast.Var("fn"), ast.Num(1)))
          "string"    - testValue(parser.sExpr, "(fn \"string\")", ast.Lst(ast.Var("fn"), ast.Str("string")))
          "symbol"    - testValue(parser.sExpr, "(fn 'symbol)", ast.Lst(ast.Var("fn"),ast.Sym("symbol")))
          "variable"  - testValue(parser.sExpr, "(fn variable)", ast.Lst(ast.Var("fn"),ast.Var("variable")))
        }
      }

    }
  }
}
