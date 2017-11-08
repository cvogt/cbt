
# CBT support in IntelliJ IDEA Scala Plugin
## Installation:

CBT support is included in IntelliJ IDEA 2017.3 and later. For 2017.2 please remove the Scala plugin and instead install this patched plugin from disk: [scala-plugin.zip](https://goo.gl/9vqurz) (sha1: `dedb029d3c4b378f85cf62a10da6425cbd1f102b`)

Make sure CBT is installed and `cbt` is available in your `PATH`.

## Requirements:
* Latest version of **CBT** from github and
* **CBT** executable available within comandline
* **Intellij IDEA** 2017.2 or later

## Features:
* Creating **CBT**-based projects
* Importing existing **CBT**-based projects 
* Running and Debugging project code using either **IDEA**'s or **CBT**'s compiler
* Running and Debugging any **CBT** build task
* **CBT** Project templates via [giter8](https://github.com/foundweekends/giter8) engine
* Navigating through libraries sources
* Editting **CBT** source code

## Others

#### Running a task
There are two possible ways 
##### The first one is to use a per task line buttons
![image](https://user-images.githubusercontent.com/16403337/29731117-e4671d38-89eb-11e7-89a4-92c784335bca.png)


##### The second is creating a **CBT** task manualy 

![image](https://user-images.githubusercontent.com/16403337/29731161-17da76e2-89ec-11e7-951a-3e8b08b60f93.png)

#### Refreshing a project
To refresh a project you need to enable a `CBT tool panel`: just press `ctrl-shift-A` and then enter `cbt`:
![image](https://user-images.githubusercontent.com/16403337/27643459-cdaee64c-5c29-11e7-8c9f-f233da861928.png)

When done a tool panel will appear on the right side: ![image](https://user-images.githubusercontent.com/16403337/27643788-b4d8998c-5c2a-11e7-929e-4e80724d4b6d.png)

#### Creating extra modules
IDEA assumes that all modules which you use are in dependencies (may be transitive) of a root module

If you want to work on some module which is not you can right-click on the directory in the project tree view and then choose `Consider as CBT module` (directory should contains `build` directory with a correct build file)

## Also see
* [Pull Request to the Intellij IDEA Scala Plugin](https://github.com/JetBrains/intellij-scala/pull/383)
* [Pull Requests to the CBT](https://github.com/cvogt/cbt/pulls?utf8=%E2%9C%93&q=%20is%3Apr%20author%3Adarthorimar%20created%3A%3E2017-06-09%20)* Organiztion **Scala**
* [GSOC Project proposal](https://docs.google.com/document/d/14BQSOKGYL9-JqrO3ZTqcCJVy2LD1JAL6nXzScxYSU-c/edit?usp=sharing)
