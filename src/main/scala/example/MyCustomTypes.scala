package example

import org.squeryl._
import org.squeryl.dsl._
import org.joda.time._
import java.sql.Timestamp
import java.sql.ResultSet
import org.squeryl.adapters.H2Adapter

object MyCustomTypes extends PrimitiveTypeMode {

  implicit val jodaTimeTEF = new NonPrimitiveJdbcMapper[Timestamp, DateTime, TTimestamp](timestampTEF, this) {
    def convertFromJdbc(t: Timestamp) = new DateTime(t)
    def convertToJdbc(t: DateTime) = new Timestamp(t.getMillis())
  }

  implicit def jodaTimeToTE(s: DateTime) = jodaTimeTEF.create(s)

  implicit val optionJodaTimeTEF = new TypedExpressionFactory[Option[DateTime], TOptionTimestamp] with DeOptionizer[DateTime, TTimestamp, Option[DateTime], TOptionTimestamp] {

    val deOptionizer = jodaTimeTEF
  }

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
