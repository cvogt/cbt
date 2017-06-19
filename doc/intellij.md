# CBT Intellij scala-plugin (fork of orginal scala-plugin)
Link: [scala.zip](https://www.dropbox.com/s/5o8fq8z6kh4stt0/Scala.zip?dl=0) (sha1: `3540583931867718e0b82d15f2038eef53d748e2`)

### Features:
* Creating cbt-based projects
* Importing existing cbt-based projects
* Running cbt-tasks (Default IDEA build task doesn't work so you have to disable it)
![cbt-task](https://user-images.githubusercontent.com/16403337/27309036-75740d5c-555a-11e7-9c9a-60e756d5e0ca.png)

### Requirements:
* Latest version of CBT from github
* Cbt executable available within comandline
* Intellij IDEA 2017.2 EAP Comunity (not sure about working on other versions)

In order to install you need to remove existing intellij-scala plugin and then choose `Install plugin from disk` and point out to the downloaded zip

### For editing CBT's source code `cache` directory should be added to `Excluded` in root module settings

![cbt](https://user-images.githubusercontent.com/16403337/27309553-3bf812fa-555d-11e7-968e-93e2c588ba6d.png)

------

The source code is here: [source](https://github.com/darthorimar/intellij-scala)

This a fork of a *JetBrains* [scala-pugin](https://github.com/jetbrains/intellij-scala)

