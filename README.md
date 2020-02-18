# Aggregate Graph Statistics

## Experimental results of the third case study

This repository contains code and instruction to reproduce the experiments presented in the paper "Aggregate Graph Statistics" by Giorgio Audrito, Danilo Pianini, Mirko Viroli, and Ferruccio Damiani; submitted to Elsevier's [Science of Computer Programming](https://www.journals.elsevier.com/science-of-computer-programming) journal.

## Requirements

Simulating 5000 mobile devices with 10 neighbors each requires up to 13GB of memory.
Underequipped hardware may not be able to complete the execution successfully.

In order to run the experiments, the Java Development Kit 11 is required.
It is very likely that they can run on any later version, but it is not guaranteed.
We test using OpenJ9 11 and latest, and OpenJDK 11 and latest.
Original testing was performed with OpenJDK 13.

In order to produce the charts, Python 3 is required.
We recommend Python 3.8.1,
but it is very likely that any Python 3 version,
and in particular any later version will serve the purpose just as well.
The recommended way to obtain it is via [pyenv](https://github.com/pyenv/pyenv).

The experiments have been designed and tested under Linux.
However, we have some muliplatform build automation in place.
Everything should run on any recent Linux, MacOS X, and Windows setup.

### Reference machine

We provide a reference Travis CI configuration to maintain reproducibility over time.
While this image: [![Build Status](https://travis-ci.org/DanySK/Experiment-2019-SCP-Graph-Statistics.svg?branch=master)](https://travis-ci.org/DanySK/DanySK/Experiment-2019-SCP-Graph-Statistics)
is green, the experiment is being maintained and,
by copying the configuration steps we perform for Travis CI in the `.travis.yml` file,
you should be able to re-run the experiment entirely.

### Automatic releases

Charts are remotely generated and made available on the project release page.
[The latest release](https://github.com/DanySK/Experiment-2019-SCP-Graph-Statistics/releases/latest)
allows for quick retrieval of the latest version of the charts.

## Running the simulations

A graphical execution of the simulation can be started by issuing the following command
`./gradlew runAllGraphic`.
Parameter defaults can be tuned in the `simulation.yml` file
Windows users may try using the `gradlew.bat` script as a replacement for `gradlew`.

The whole simulation batch can be executed by issuing `./gradlew runAllBatch`.
**Be aware that it may take a very long time**, from several hours to weeks, depending on your hardware.
If you are under Linux, the system tries to detect the available memory and CPUs automatically, and parallelize the work.

## Generating the charts

In order to speed up the process for those interested in observing and manipulating the existing data,
we provide simulation-generated data directly in the repository.
Generating the charts is matter of executing the `process.py` script.
The enviroment is designed to be used in conjunction with pyenv.

### Python environment configuration

The following guide will start from the assumption that pyenv is installed on your system.
First, install Python by issuing

``pyenv install --skip-existing 3.8.1``

Now, configure the project to be interpreted with exactly that version:

``pyenv local 3.8.1``

Update the `pip` package manager and install the required dependencies.

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### Data processing and chart creation

This section assumes you correctly completed the required configuration described in the previous section.
In order for the script to execute, you only need to launch the actual process by issuing `python process.py`
