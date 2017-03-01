import cbt._

class Build(val context: Context) extends Proguard{
  def proguard = proguardKeep( (Nil, """
    public class proguard_example.Main{
      public void main(java.lang.String[]);
    }
  """ ) )
}
