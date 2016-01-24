package strips.read

import supplies.sexpr._

abstract class TypedSReader[T](l : Seq[ast.Expr]) {
  /**
  * Number of positional arguments expected (not including keyvals)
  * @type Int
  */
  def positionals : Int

  def ordered : Seq[ast.Expr] = l.take(positionals).toList
  def hashed  : Map[ast.Expr, Any]
  = l.drop(positionals).grouped(2).map(a => a(0) -> a(1).value).toMap

  def apply : Option[T]
}

abstract class ScopedData(scope : String) {}

case class OntDefineType(name : String, data : Map[String, Any]) extends ScopedData("ont") {
  def parent   : String = data.get("parent").map(_.asInstanceOf[String]).getOrElse("ont::nil")
  def wordnet  : List[String] = data.get("wordnet-sense-keys").map(_.asInstanceOf[List[String]]).getOrElse(List())
  def comment  : String = data.get("comment").map(_.asInstanceOf[String]).getOrElse("")
  def sem      : ast.Lst= data.get("sem").map(_.asInstanceOf[ast.Lst]).getOrElse(ast.Lst())
  def arguments: ast.Lst= data.get("arguments").map(_.asInstanceOf[ast.Lst]).getOrElse(ast.Lst())
}

trait F_

case class FeatureDeclaration(label : String) extends F_{
  def values : List[Feature] = List()//get the values from static data, unfixed
}

case class FeatureRange(label : String, _values : List[Feature]) extends F_ { //restricted values
  def values : List[Feature] = _values.map(_.fix)
}

case class Feature(label : String, name : String, static : Boolean = false) extends F_ {
  def values : List[Feature] = List() //Get the values from static data
  def fix : Feature = this.copy(static = true) //Fixable
}

/**
*  SemType could be a feature list (in which case overrides will always be empty) or the value of a ont type
*  This can also become a monoid (sort of).  Name and inherits aren't parts of Monoid[Sem]
*  Don't need to know the parent ont-type at read time since we can just step up the hierarchy at access time
*/
case class SemInherits(from : String) extends F_ //Can't deal with something like (? rst F::A (F::x F::v) F::B) <– ie, modified inherits
case class FeatureDefault(value : Feature) extends F_
case class FeatureAssignment(value : Feature) extends F_

case class Sem(
  name : String,
  inherits : List[SemInherits] = List(), //If name is defined in feature-list then use that
  features : List[FeatureDeclaration] = List(), //The feature trees which must be represented
  defaults : List[FeatureDefault] = List(),
  restricted : List[FeatureRange] = List(), //Features that are restricted to a subset of their values
  overrides : List[FeatureAssignment] = List()
) {
  def valid : Boolean = true
  def complete : Boolean = true
}

object Sem {
  def apply(name : String, v : List[F_]) : Sem = {
    val inherits = v.collect{case a : SemInherits => a}
    val features = v.collect{case a : FeatureDeclaration => a}
    val defaults = v.collect{case a : FeatureDefault => a}
    val restricted=v.collect{case a : FeatureRange => a}
    val overrides= v.collect{case a : FeatureAssignment => a}

    Sem(name, inherits, features, defaults, restricted, overrides)
  }
}

class SemTypeReader(l : Seq[ast.Expr], name : String) extends TypedSReader[Sem](l) {
  override val positionals : Int = 0
  override val ordered : Seq[ast.Expr] = l.toList
  override val hashed : Map[ast.Expr, Any] = Map()

  override def apply() : Option[Sem] = {
    val p : List[List[F_]] = l.map(y => {
      y match{
        case ast.Var(n) => List(SemInherits(n))
        case l2: ast.Lst=> l2.elements.toList match {
          //Can be either a feature declr (!?) or a optionlist
          case ast.Var("?") :: ast.Var(v) :: rest => rest.map(x => SemInherits(x.asInstanceOf[ast.Var].value)).toList
          case ast.Var(f) :: expr => {
            expr match {
              case ast.Var("?") :: ast.Var(v) :: rest => List(FeatureRange(f, rest.map(x => Feature(f, x.asInstanceOf[ast.Var].value))))
              case List(ast.Var(r)) => List(FeatureAssignment(Feature(f, r)))
              case _ => List[F_]()
            }
          }
          case _ => List[F_]()
        }
        case _ => List[F_]()
      }
    }).toList

    if (p.size == 0) None
    else Some(Sem(name, p.flatten))
  }
}

class OntDefineTypeReader(l : Seq[ast.Expr]) extends TypedSReader[OntDefineType](l) {
  override def positionals : Int = 2
  override def apply() : Option[OntDefineType] = {
    ordered match {
      case ast.Var("define-type") :: ast.Var(t) :: rest => {
        Some(OntDefineType(t, hashed.map(a => a._1.asInstanceOf[ast.Key].asVar.value -> a._2)))
      }
      case _ => None
    }
  }
}
