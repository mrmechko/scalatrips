package supplies.dataops.backend

/*
  This segment contains the necessary traits for data ops.
  JS implementations may 1. be different to jvm, 2. unnecessary
 */

trait datasource {
  /**
   * where to read from
   */
  val sourceuri : String

  /**
   * actually dump the text
   * @return String
   */
  def read : String
}

trait datacontent {
  /**
   * Some data to be written
   */
  val data : String

  /**
   * write this data to a file
   * @param targeturi : The target to write this data to
   * @return Boolean
   */
  def to(targeturi : String) : Boolean
}

trait DataOpsFile {
  def getString(sourceuri : String) : datasource
  def dumpString(data : String) : datacontent
}


trait stringtransforms {
  def transform(content : String) : String
  def operation : String
}

trait TransformData extends stringtransforms {
  val pipeline : List[stringtransforms]
  def transform(content : String) : String = pipeline.foldLeft(content)((a, b) => b.transform(a))
  val operation : String = pipeline.map(_.operation).mkString(" -> ")
}

package object transformers {
  case object downcase extends stringtransforms {
    override def transform(content : String) : String = content.toLowerCase
    val operation : String = "downcase"
  }
}
