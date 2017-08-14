# CBT Intellij scala-plugin (fork of orginal scala-plugin)
Link: [scala-plugin.zip](https://goo.gl/9vqurz) (sha1: `dedb029d3c4b378f85cf62a10da6425cbd1f102b`)

### Features:
* Creating CBT-based projects
* Importing existing CBT-based projects 
* Editting CBT source code
* Running and debugging arbitrary build task
* Navigating through libraries sources


### Requirements:
* Latest version of CBT from github
* CBT executable available within comandline
* Intellij IDEA 2017.2

In order to install you need to remove existing intellij-scala plugin and then choose `Install plugin from disk` and point out to the downloaded zip

### Others

#### Refreshing a project
To refresh o project you need to enable a `CBT tool panel`: just press `ctrl-shift-A` and then enter `cbt`:
![image](https://user-images.githubusercontent.com/16403337/27643459-cdaee64c-5c29-11e7-8c9f-f233da861928.png)

When done a tool panel will aperas at the right side: ![image](https://user-images.githubusercontent.com/16403337/27643788-b4d8998c-5c2a-11e7-929e-4e80724d4b6d.png)

#### Creating extra modules
IDEA assumes that all modules which you use are in dependencies (may be transitive) of a root module

If you want to work on some module which is not you can right-click on the directory in the project tree view and then choose `Consider as CBT module` (directory should contains `build` directory with a correct build file)

------

The source code is here: [source](https://github.com/darthorimar/intellij-scala)

This a fork of a *JetBrains* [intellij-scala](https://github.com/jetbrains/intellij-scala) and soon should be merged into it

Releated PR in IDEA Scala Plugin [here](https://github.com/JetBrains/intellij-scala/pull/383)

**Use and leave feedback** :metal: 

