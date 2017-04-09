import org.scalatest._

class Test extends FunSpec with Matchers {
  describe( "Disjunction" ) {
    it( "should work in all versions" ) {
      Disjunction.intOrString shouldBe Right( "It works!" )
    }
  }
}
