# CBT AWS lambda compilation

### Usage
To compile a directory on Lambda, import `cbt.cluster.lambda` in your `build.scala` file.
Each directory dependency to be compile on lambda must be a case class that extends `OnAwsLambda`
and you must provide and S3 bucket name that Lambda can use to store your files and code.

```
case class Project(implicit val logger: Logger) extends OnAwsLambda (
  new BasicBuild(context.copy(projectDirectory = new File([some project directory]) ))
) {
  def bucketName = [some bucket]
}
```

Finally add a new instance of project to your dependencies:
`override def dependencies = super.dependencies ++ Seq(new Project())`

You must use a run the compilation directly on the JVM (i.e. `cbt direct compile`).


Known issues:
- Cached dependencies are currently not downloaded.
- Requires AWS CLI to be set up.
- Connection throws timeout exception but continues
- First time compilation throws exception (rerun)
- Errors in compilation aren't propagated
