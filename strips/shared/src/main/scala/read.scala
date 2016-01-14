package strips.read

import supplies.sexpr._

abstract class TypedSReader[T](l : Seq[ast.Expr]) {
  /**
   * Number of positional arguments expected (not including keyvals)
   * @type Int
   */
  def positionals : Int

  def ordered : Seq[ast.Expr] = l.take(positionals)
  def hashed  : Map[ast.Expr, ast.Expr]
    = l.drop(positionals).grouped(2).map(a => a(0) -> a(1)).toMap

  def apply : Option[T]
}

abstract class ScopedData(scope : String) {}

case class OntDefineType(name : String, data : Map[String, ast.Expr]) extends ScopedData("ont")

class OntDefineTypeReader(l : Seq[ast.Expr]) extends TypedSReader[OntDefineType](l) {
  override def positionals : Int = 2
  override def apply : Option[OntDefineType] = {
    ordered match {
      case ast.Var("define-type") :: ast.Var(t) :: Nil => {
        Some(OntDefineType(t, hashed.map(a => a._1.asInstanceOf[ast.Key].asVar.value -> a._2)))
      }
      case _ => None
    }
  }
}
