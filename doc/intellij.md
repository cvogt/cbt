# CBT Intellij scala-plugin (fork of orginal scala-plugin)
Link: [scala-plugin.zip](https://www.dropbox.com/s/508v9ypypvrpni9/scala-plugin.zip?dl=0) (sha1: `a330220b831bedea8863b30ba15c7d8cdedfa740`)

### Features:
* Creating CBT-based projects
* Importing existing cbt-based projects 
* Editting CBT source code (will be a little better when #536 will be accepted)

    **Cache directory should be added to Excluded in root module setting**
* Running cbt-tasks (You better do sibale IDEA default build task) ![cbt-task](https://user-images.githubusercontent.com/16403337/27309036-75740d5c-555a-11e7-9c9a-60e756d5e0ca.png)
* Running and debugging CBT-based projects
* Navigating through libraries sources



### Requirements:
* Latest version of CBT from github
* CBT executable available within comandline
* Intellij IDEA 2017.2 Community Public Preview (not sure about working on other versions)

In order to install you need to remove existing intellij-scala plugin and then choose `Install plugin from disk` and point out to the downloaded zip

### Others

#### Refreshing a project
To refresh o project you need to enable a `CBT tool panel`: just press `ctrl-shift-A` and then enter `cbt`:
![image](https://user-images.githubusercontent.com/16403337/27643459-cdaee64c-5c29-11e7-8c9f-f233da861928.png)

When done a tool panel will aperas at the right side: ![image](https://user-images.githubusercontent.com/16403337/27643788-b4d8998c-5c2a-11e7-929e-4e80724d4b6d.png)

#### Creating extra modules
For now IDEA assumes that all modules which you use are in dependencies(may be transitive) of a root module

If you want to work on some module which is not you can right-click on the directory in the project tree view and then choose `Consider as CBT module` (directory should contains `build` directory with a correct build file)

#### Correct build files
For now plugin assumes that if you import some project(or may be just refresh it) all build files do not contain errors. Otherwise it would not just be imported/refreshed

------

The source code is here: [source](https://github.com/darthorimar/intellij-scala)

This a fork of a *JetBrains* [scala-pugin](https://github.com/jetbrains/intellij-scala)

