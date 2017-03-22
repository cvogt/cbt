import foo._
object Main extends App {
  println(
    person.Person(
      Some("name"),
      Some(123),
      Seq(
        address.Address(
          Some("High Street")
        )
      )
    )
  )
}
