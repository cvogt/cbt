import www.example.com.IPO

object Usage extends App {
  val shipTo = scalaxb.fromXML[IPO.Address](<shipTo>
      <name>foo</name>
      <street>1537 Paper Street</street>
      <city>Wilmington</city>
    </shipTo>)
  println(shipTo.toString)
}
