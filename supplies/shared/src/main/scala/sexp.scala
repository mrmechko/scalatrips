package supplies
/**
* Readers should be implemented using FastParse.
*
* The XML reader is already written.  This allows the same parser to be used on
* both JVM and JS sides
*/

package object sexpr {

  /**
   * These are some simple matchers.  If you need to debug your files or do any
   * error checking, you should call parser.parseAll.parse.  I'll rename that
   * soon
   */
  case object slist {
    /**
     * Succeeds only if the entire String is parsed
     * @param data :             String
     * @return a seq of expressions
     */
    def apply(data : String) : Option[Seq[ast.Expr]] = parser.parseAll.parse(data) match {
      case fastparse.all.Parsed.Success(v, i) => Some(v)
      case _ => None
    }
  }

  /**
   * Succeeds only if the entire String is parsed
   * @param data :             String
   * @return an expression
   */
  case object sone {
    def apply(data : String) : Option[ast.Expr] = parser.parse.parse(data) match {
      case fastparse.all.Parsed.Success(v, i) if i == data.size => Some(v)
      case _ => None
    }
  }

  object unpack {
      class read[T](reader0 : ast.Expr => Option[T]) {
        def apply(exp : ast.Expr) : Option[T] = reader0(exp)
      }

      val string = new read[String](
        _ match {
          case ast.Str(value) => Some(value)
          case _ => None
        }
      )

      val double = new read[Double](
        _ match {
          case ast.Num(value) => Some(value)
          case _ => None
        }
      )

      val list = new read[List[ast.Expr]](
        _ match {
          case e : ast.Lst => Some(e.elements.toList)
          case _ => None
        }
      )

      val listString = new read[List[String]](
        a => {
          list(a) match {
            case Some(e : List[ast.Expr]) => {
              val l = e.map(string(_))
              if(l.forall(_.isDefined)) Some(l.flatten)
              else None
            }
            case _ => None
          }
        }
      )


  }

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
    case class Lst(value :  (Expr)*) extends AnyVal with Expr {
      def elements : Seq[Expr] = value
    }
    // Is there any case in which its reasonable for there to be a non-Kv after a Kv?
    case class Key(value : java.lang.String) extends AnyVal with Atom {
      def asVar = Var(value)
    }

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
    val reserved = "\"\\ \n\t()#[],{}!'*;|"
    val symbolOp = "+-*^&%!><?\\"

    val Whitespace = NamedFunction(" \t\n".contains(_: Char), "Whitespace")
    val Digits = NamedFunction('0' to '9' contains (_: Char), "Digits")
    val StringChars = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
    val TokenChars = NamedFunction(!reserved.contains(_: Char), "TokenChars")//is this a regex?
    val TokenStart = NamedFunction(!(reserved+":" + ("0123456789")).contains(_: Char), "TokenStart")

val strChars = P( CharsWhile(StringChars) )

    val inlineComment = P( ";" ~ (!"\n" ~/ AnyChar).rep ~/ "\n")
    val hashpipe      = P( "#|")
    val pipehash      = P( "|#" )
    val hpcontent : P[Unit]     = P( (&("#|") ~ hpcomment) | (!"|#" ~ AnyChar) )
    val hpcomment : P[Unit]     = P( hashpipe ~ (hpcontent).rep ~ pipehash )
    val space         = P( (CharsWhile(Whitespace) | NoCut(inlineComment) | NoCut(hpcomment)).rep )
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


    val tokenChars = P( CharsWhile(TokenChars) ) //Tokens should probably start with a letter of some sort?
    val tokenStart = P( CharsWhile(TokenStart) )
    val string =
      P( space ~ "\"" ~ (strChars | escape).rep.! ~ "\"").map(ast.Str)

    val symbol =
      P( space ~ "'" ~ (tokenChars).rep.!).map(ast.Sym)

    val variable =
      P( space ~ tokenStart.rep(1).! ~ (tokenChars).rep.!).map(s => ast.Var(s._1 + s._2)) //make sure the first element is not a restricted char

    val key = P( space ~ ":"~variable.map(_.value)).map(ast.Key)

    val lst = P( space ~ "(" ~ sExpr.rep(sep = space) ~ space ~ ")" ~ space ).map(s => {
      ast.Lst(s :_*)
    })

    val atom: P[ast.Atom] = P(
      (string | number | variable | symbol | key)
    )

    val sExpr: P[ast.Expr] = P(
      space ~ (NoCut(lst) | atom) ~ space
    )

    val parse: P[ast.Expr] = space ~ (atom | NoCut(lst)) ~ space//not necessary

    val parseAll : P[Seq[ast.Expr]] = P( space ~ parse.rep(sep=space) ~ space )//.map(x => List(x :_* ))
  }
}
