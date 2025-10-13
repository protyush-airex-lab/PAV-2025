# PAV 2025 Course Project

## Objective
To build a tool to statically perform analysis for JAVA programs.

We will use the static analysis framework Soot to build the analysis.

## Pre-requisites

Make sure that your system has **Java 21** and **Maven** installed
```
sudo apt install openjdk-21-jdk maven
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

## Running

To build the jar file of your project, cd to the location of the `pom.xml` file and run:
```
mvn clean package
```
A jar file named `Analysis-jar-with-dependencies.jar` will be created inside the `target` folder

Run you project using:
```
java -jar target/Analysis-jar-with-dependencies.jar dirname mainclass tclass tmethod
``` 