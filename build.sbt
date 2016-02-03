enablePlugins(ScalaJSPlugin)

lazy val commonSettings = Seq(
  organization := "org.mechko",
  version := repo.version,
  scalaVersion := "2.11.7",
  scalacOptions := Seq("-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-feature"),
// Sonatype
  publishArtifact in Test := false,
/*publishTo <<= version { (v: String) =>
  Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
},*/
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided",
    "com.lihaoyi" %%% "utest" % "0.3.1" % "test"
  ),
  libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
  unmanagedSourceDirectories in Compile ++= Seq(baseDirectory.value / ".." / "shared" / "src" / "main" / "scala-2.11"),
  unmanagedResourceDirectories in Test ++= Seq(baseDirectory.value / ".." / ".." / "testfiles"),

  scalaJSStage in Global := FastOptStage,
  autoCompilerPlugins := true,
//  scalacOptions += "-Xlog-implicits",
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2")//,
/*
  pomExtra :=
  <url>https://github.com/lihaoyi/upickle</url>
    <licenses>
      <license>
        <name>MIT license</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git://github.com/lihaoyi/upickle.git</url>
      <connection>scm:git://github.com/lihaoyi/upickle.git</connection>
    </scm>
    <developers>
      <developer>
        <id>lihaoyi</id>
        <name>Li Haoyi</name>
        <url>https://github.com/lihaoyi</url>
      </developer>
    </developers>
    */
)

lazy val badlands = crossProject.
  settings(commonSettings : _*).
  settings(
    name := "badlands",
    libraryDependencies ++= Seq(
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
      "com.github.mpilquist" %%% "simulacrum" % "0.6.1",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
    )
  )

val badlandsJS = badlands.js
val badlandsJVM = badlands.jvm

lazy val supplies = crossProject.
  settings(commonSettings: _*).
  settings(
    name := "supplies",
    libraryDependencies += "com.lihaoyi" %%% "fastparse" % "0.3.4"
  )
val suppliesJS = supplies.js
val suppliesJVM= supplies.jvm

//figure out how to remove the shared/jvm/js requirement
lazy val strips = crossProject.//.dependsOn(someotherproject)
  dependsOn(supplies % "compile->compile;test->test").
  settings(commonSettings: _*).
  settings(
    // other settings
    name := "strips"
  )//.jsSettings().jvmSettings()

val stripsJS = strips.js
val stripsJVM= strips.jvm

lazy val diesel = crossProject.
  dependsOn(strips % "compile->compile;test->test").
  settings(commonSettings: _*).
  settings(
    // other settings
    name := "diesel"
  )
val dieselJS = diesel.js
val dieselJVM= diesel.jvm

lazy val upickleReadme= scalatex.ScalatexReadme(
  projectId = "suppliesReadme",
  wd = file(""),
  url = "https://github.com/mrmechko/scalatrips/tree/master",
  source = "Readme"
).settings(
  (unmanagedSources in Compile) += baseDirectory.value/".."/"project"/"repo.scala"
)
