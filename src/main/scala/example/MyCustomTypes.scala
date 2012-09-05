package example

import org.squeryl._
import org.squeryl.dsl._
import org.joda.time._
import java.sql.Timestamp
import java.sql.ResultSet
import org.squeryl.adapters.H2Adapter

/**
 * PrimitiveTypeMode being a trait, can be extended to   
 * support additionnal field types than what the native JDBC. 
 *
 *  To support a non JDBC native type, one extends PrimitiveTypeMode
 *  and adds 4 new implicits in it's scope.   Then all aspects 
 *  of the Query DSL that are available for the native type 
 *  becomes available for the new type 
 *  
 *  
 */
object MyCustomTypes extends PrimitiveTypeMode {

  /**
   * The 3 type arguments are :
   * 1) Timestamp : the native JDBC type that will back our custom type (in this case JodaTime's DateTime)
   * 2) The new type we want to support
   * 3) One of the sealed traits defined here :  
   *   https://github.com/max-l/Squeryl/blob/AST-lifting-overhaul/src/main/scala/org/squeryl/dsl/TypedExpression.scala#L28
   * This will define the behavior of the new type, we want that in the DSL it behaves like 
   * a timestamp, this will for example enable comparison operators (<, >, between, etc...)
   * as well as min, max functions, if we chose TBoolean, then min max is not available,
   * if we chose TBigDecimal (of TFloat), then avg, sum, etc become available.  
   *  
   */
  
  implicit val jodaTimeTEF = new NonPrimitiveJdbcMapper[Timestamp, DateTime, TTimestamp](timestampTEF, this) {
    
    /**
     * Here we implement functions fo convert to and from the native JDBC type
     */
    
    def convertFromJdbc(t: Timestamp) = new DateTime(t)
    def convertToJdbc(t: DateTime) = new Timestamp(t.getMillis())
  }

  /**
   * We define this one here to allow working with Option of our new type, this allso 
   * allows the 'nvl' function to work  
   */
  implicit val optionJodaTimeTEF = 
    new TypedExpressionFactory[Option[DateTime], TOptionTimestamp] 
      with DeOptionizer[Timestamp, DateTime, TTimestamp, Option[DateTime], TOptionTimestamp] {

    val deOptionizer = jodaTimeTEF
  }
  
  /**
   * the following are necessary for the AST lifting  
   */
  implicit def jodaTimeToTE(s: DateTime) = jodaTimeTEF.create(s)  

  implicit def optionJodaTimeToTE(s: Option[DateTime]) = optionJodaTimeTEF.create(s)

}

class TimestampTester(val time: DateTime, val optionalTime: Option[DateTime]) {
  def this(t: DateTime) = this(t, None)
}

import MyCustomTypes._

object TimestampTesterSchema extends Schema {

  val timestampTester = table[TimestampTester]
}

object JodaTimeTests {

  import TimestampTesterSchema._

  def main(args: Array[String]): Unit = {

    Class.forName("org.h2.Driver")
    SessionFactory.concreteFactory = Some(() =>
      Session.create(
        java.sql.DriverManager.getConnection("jdbc:h2:~/test", "sa", ""),
        new H2Adapter))
    transaction {
      try { drop } catch { case e: Exception => {} }
      create
    }

    test1
  }

  def test1 = transaction {

    val d = (new DateTime)

    val b10 = d.minusDays(10)
    val b5 = d.minusDays(5)
    val a5 = d.plusDays(5)

    timestampTester.insert(new TimestampTester(b10))
    timestampTester.insert(new TimestampTester(b5, Option(new DateTime)))
    timestampTester.insert(new TimestampTester(a5))

    val x1 =
      from(timestampTester)(tt =>
        where(tt.time < b5)
        select (&(tt.time)))
        
    assert(x1.size == 1)

    println(x1.mkString("\n"))

    val x2 =
      from(timestampTester)(tt =>
        where(tt.time >= b5)
          select (tt))
          
    assert(x2.size == 2)

    println(x2.map(_.time).mkString("\n"))
  }
}
