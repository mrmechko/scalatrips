package supplies
/**
* Readers should be implemented using FastParse.
*
* The XML reader is already written.  This allows the same parser to be used on
* both JVM and JS sides
*/

package object sexpr {
  object ast {

    sealed trait Expr extends Any {
      def value : Any
      def apply(i: Int): Expr = this.asInstanceOf[Lst].value(i)
      //def apply(s: java.lang.String): Val =
      //  this.asInstanceOf[Obj].value.find(_._1 == s).get._2 //This is a hash lookup?
      //  Alternatively, if we can't follow a Kv with anything else...
    }

    sealed trait Atom extends Any with Expr

    /**
    * Compound Exprs
    */
    case class Lst(value :  (Expr)*) extends AnyVal with Expr
    // Is there any case in which its reasonable for there to be a non-Kv after a Kv?
    case class Kv(value : (java.lang.String, Expr)) extends AnyVal with Expr

    /**
    * Atomic Expressions
    */

    case class Sym(value : java.lang.String) extends AnyVal with Atom
    case class Str(value : java.lang.String) extends AnyVal with Atom
    case class Num(value : Double) extends AnyVal with Atom
    case class Var(value : java.lang.String) extends AnyVal with Atom {
      //scope extraction
    }

  }

  /**
  * That thing in the fast parse example
  */
  case class NamedFunction[T, V](f: T => V, name: String) extends (T => V){
    def apply(t: T) = f(t)
    override def toString() = name
  }

  object parser {
    import fastparse.all._
    val reserved = "\"\\ \n\t()#[],{}?!'*;"

    val Whitespace = NamedFunction(" \n".contains(_: Char), "Whitespace")
    val Digits = NamedFunction('0' to '9' contains (_: Char), "Digits")
    val StringChars = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
    val TokenChars = NamedFunction(!reserved.contains(_: Char), "TokenChars")//is this a regex?
    val TokenStart = NamedFunction(!(reserved+":" + ('0' to '9')).contains(_: Char), "TokenStart")

    val space         = P( CharsWhile(Whitespace).? )
    val SPACE         = P( CharsWhile(Whitespace).! )
    val digits        = P( CharsWhile(Digits))
    val exponent      = P( CharIn("eE") ~ CharIn("+-").? ~ digits )
    val fractional    = P( "." ~ digits )
    val integral      = P( "0" | CharIn('1' to '9') ~ digits.? )

    val number = P( CharIn("+-").? ~ integral ~ fractional.? ~ exponent.? ).!.map(
      x => ast.Num(x.toDouble)
    )

    val hexDigit      = P( CharIn('0'to'9', 'a'to'f', 'A'to'F') )
    val unicodeEscape = P( "u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit )
    val escape        = P( "\\" ~ (CharIn("\"/\\bfnrt") | unicodeEscape) )

    val strChars = P( CharsWhile(StringChars) )
    val tokenChars = P( CharsWhile(TokenChars) ) //Tokens should probably start with a letter of some sort?
    val tokenStart = P( CharsWhile(TokenStart) )
    val string =
      P( space ~ "\"" ~ (strChars | escape).rep.! ~ "\"").map(ast.Str)

    val symbol =
      P( space ~ "'" ~ (tokenChars).rep.!).map(ast.Sym)

    val variable =
      P( space ~ tokenStart.rep(1).! ~ (tokenChars).rep.!).map(s => ast.Var(s._1 + s._2)) //make sure the first element is not a restricted char

    val kv = P( space ~ ":"~variable.map(_.value) ~/ sExpr ).map(s => ast.Kv((s._1, s._2)))

    val lst = P( space ~ "(" ~/ sExpr.rep(sep = space) ~ ")" ).map(s => {
      println(s)
      ast.Lst(s :_*)
    })

    val atom: P[ast.Atom] = P(
      (string | number | variable | symbol)
    )

    val sExpr: P[ast.Expr] = P(
      space ~ (NoCut(kv) | NoCut(lst) | atom) ~ space
    )

    val parse: P[ast.Expr] = P(
      space ~ (atom | "("~ sExpr ~")")
    )
  }
}
