import ai.x.diff
import org.eclipse.jgit.lib.Ref
import com.spotify.missinglink.ArtifactLoader
object Main extends App{
  println(diff.DiffShow.diff("a","b"))
}
