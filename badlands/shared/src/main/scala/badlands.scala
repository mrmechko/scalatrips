package badlands.typeclasses

import scala.language.implicitConversions
import simulacrum._

@typeclass trait DataMerge[A] {
  @op("-><-") def combine(x: A, y: A): A
}

/*
 *  How to do variances and type bounds here?
 *  Also, I feel like this is an existing structure?
 */

@typeclass trait DataCombine[A, B] {
  @op("<=:") def addTo(result : A, data : B) : A
  def empty : A
}

object implicits {
  implicit val DataMergeInt: DataMerge[Int] = new DataMerge[Int] {
    def combine(x: Int, y: Int) = x + y
  }
}
