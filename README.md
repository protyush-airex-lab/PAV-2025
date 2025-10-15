# PAV 2025 Course Project

## Objective
To build a tool to statically perform `Intra-procedural Point-to Analysis` for JAVA programs.

We will use the static analysis framework [Soot](https://soot-oss.github.io/soot/) to build the analysis.

## Pre-requisites

Make sure that your system has **Java 21**, **Maven** and **Graphviz** installed
```
sudo apt install openjdk-21-jdk maven graphviz
```

## Repo initialization

Do not clone this repo directly. You will not be able to push changes.

Instead "Fork" this repo under your username. Name it as "2025-PAV-FirstName"
(eg: "2025-PAV-Alvin").

Make the fork you created as **Private**. Then give access to Alan (@AlanJojo) and Atharv (@atharv.desai) to this repository as **Developers**.

Now clone that repo on to your system:
```bash
$ git clone git@gitlab.com:USERNAME/2025-PAV-FirstName.git
```
This will create a directory `2025-PAV-FirstName` in your system. This is your local workspace directory.

## Public testcases
The public testcase functions are provided in the folder `src/main/java/test/Test.java`

You can add your own testcases in this file by creating them as functions inside the class `Test`

## Running

To build and run your analysis (in this case, generate the CFG for all of the functions inside the class `Test`), cd to the location of the `pom.xml` file and run:
```
mvn -q clean package exec:java
```

You will see the Jimple IR of all the methods in the class `Test` as output in your terminal.

CFG graphs for all the methods will be generated as dot output files inside the folder `output/`

If you have `graphviz` installed in your system, this will also generate PNG files from the dot files.