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

case class OntDefineType(name : String, data : Map[String, Any]) extends ScopedData("ont")

class OntDefineTypeReader(l : Seq[ast.Expr]) extends TypedSReader[OntDefineType](l) {
  override def positionals : Int = 2
  override def apply() : Option[OntDefineType] = {
    println(ordered)
    ordered match {
      case ast.Var("define-type") :: ast.Var(t) :: rest => {
        Some(OntDefineType(t, hashed.map(a => a._1.asInstanceOf[ast.Key].asVar.value -> a._2)))
      }
      case _ => None
    }
  }
}
