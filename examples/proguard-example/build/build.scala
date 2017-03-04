import cbt._

class Build(val context: Context) extends ProGuard{
  def proguard = ProGuard( (Nil, """
    public class proguard_example.Main{
      public void main(java.lang.String[]);
    }
  """ ) )
}
