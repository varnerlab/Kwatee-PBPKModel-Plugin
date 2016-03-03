Introduction
----

The Kwatee Physiologically Based Pharmacokinetic (PBPK) plugin generates enhanced PBPK models.

Requirements
---

* `Julia`: The PBPK model equations are generated in the [Julia](http://julialang.org) programming language.
* `Sundials package`: The Kwatee PBPK plugin generates model equations which are solved using the [Sundials](https://github.com/JuliaLang/Sundials.jl/blob/master/README.md) package for [Julia](http://julialang.org). In the future, the PBPK plugin will also support the ODE package (which provides solvers similar to the ODE suite of Matlab). However, the current version generates only Sundials compatible model equations.

How do I generate code?
---

The plugin can be used by putting the jar file into the plugins subdirectory of your [Kwatee server installation](https://github.com/varnerlab/KwateeServer). You can either use the current compiled jar file found in the `./build/libs` subdirectory of this repository or you can compile the source code yourself using the [Gradle](http://gradle.org) build system by executing:

~~~
gradle jar
~~~

in the root directory of this repository. __Note:__ If you choose to compile from source, you need both the Kwatee server and libSBML jars on your Java classpath. The `build.gradle` file in the repository automatically adds all jars found in the `libs` subdirectory to your classpath.

Model and job files
----

The biology and connectivity of your PBPK model is specified using a simple comma delimated file structure called a Varner flat file (VFF). An example VFF for a six-comparment PBPK model with a few biochemical reactions is included in the `examples` subdirectory of the [Kwatee server](https://github.com/varnerlab/KwateeServer/tree/master/examples/cell-free-example) and shown below:

~~~
// ============================================================ //
// Test model file for the Kwatee PBPK plugin
// Author: J. Varner
// Version: 1.0
// ============================================================ //

// ======================================================================== //
// Compartment connections -
#pragma handler_class = VLCGPBPKCompartmentConnectionParserDelegate
// ======================================================================== //

// Main compartments -
C1C2,compartment_1,compartment_2,0,inf;
C2C3,compartment_2,compartment_3,0,inf;
C3C4,compartment_3,compartment_4,0,inf;
C3C5,compartment_3,compartment_5,0,inf;
C4C1,compartment_4,compartment_1,0,inf;
C5C1,compartment_5,compartment_1,0,inf;

// Wound compartment -
C1W,compartment_1,wound_compartment,-inf,inf;
Loss_W,wound_compartment,[],0,inf;

// Fluid interventions -
addition_of_fluid,[],compartment_1,0,inf;

// ======================================================================== //
// Compartment biochemistry -
#pragma handler_class = VLCGPBPKCompartmentBiochemistryParserDelegate
// ======================================================================== //
conversion_of_A_to_B_slow,*,E,A,B,0,inf;
conversion_of_A_to_B,*,E,A,B,0,inf;
generation_of_A,compartment_5,[],[],A,0,inf;
generation_of_II,*,[],[],II,-inf,inf;
generation_of_E,wound_compartment,[],[],E,-inf,inf;
clearance_of_A,compartment_4,[],A,[],0,inf;
clearance_of_B,compartment_4,[],B,[],0,inf;
clearance_of_AI,compartment_4,[],AI,[],0,inf;

// B-driven deactivation -
activation_of_I,*,B,II,AI,0,inf;

// ======================================================================== //
// Compartment biochemistry control
#pragma handler_class = VLCGPBPKCompartmentBiochemistryControlParserDelegate
// ======================================================================== //
activation_of_B_by_B_B,*,B,conversion_of_A_to_B,activation;
deactivation_of_B_Gen_AI,*,AI,conversion_of_A_to_B,inhibition;
deactivation_of_B_Gen_AI_slow,*,AI,conversion_of_A_to_B_slow,inhibition;

// ======================================================================== //
// Special species rules
#pragma handler_class = VLCGPBPKSpeciesRulesParserDelegate
// ======================================================================== //
E,fixed;
II,fixed;
~~~


__Job configuration files__:In addition to model specification files, to succesfully generate code you will need a job specification file by default given the filename `Configuration.xml`. Job configuration files define paths and other data required by Kwatee. A typical job configuration file is:

~~~
<?xml version="1.0" encoding="UTF-8"?>
<Model username="jeffreyvarner" model_version="1.0" model_type="PBPK-JULIA" large_scale_optimized="false" model_name="TEST_MODEL">
  <Configuration>
    <ListOfPackages>
        <package required="YES" symbol="INPUT_HANDLER_PACKAGE" package_name="org.varnerlab.kwatee.pbpkmodel"></package>
        <package required="YES" symbol="OUTPUT_HANDLER_PACKAGE" package_name="org.varnerlab.kwatee.pbpkmodel"></package>
    </ListOfPackages>
    <ListOfPaths>
        <path required="YES" symbol="KWATEE_INPUT_PATH" path_location="/Users/jeffreyvarner/Desktop/julia_work/pbpk_model_example/"></path>
        <path required="YES" symbol="KWATEE_SOURCE_OUTPUT_PATH" path_location="/Users/jeffreyvarner/Desktop/julia_work/pbpk_model_example/src/"></path>
        <path required="YES" symbol="KWATEE_NETWORK_OUTPUT_PATH" path_location="/Users/jeffreyvarner/Desktop/julia_work/pbpk_model_example/network/"></path>
        <path required="YES" symbol="KWATEE_DEBUG_OUTPUT_PATH" path_location="/Users/jeffreyvarner/Desktop/julia_work/pbpk_model_example/debug/"></path>
        <path required="YES" symbol="KWATEE_SERVER_ROOT_DIRECTORY" path_location="/Users/jeffreyvarner/Desktop/KWATEEServer-v1.0/"></path>
        <path required="YES" symbol="KWATEE_SERVER_JAR_DIRECTORY" path_location="/Users/jeffreyvarner/Desktop/KWATEEServer-v1.0/dist/"></path>
        <path required="YES" symbol="KWATEE_PLUGINS_JAR_DIRECTORY" path_location="/Users/jeffreyvarner/Desktop/KWATEEServer-v1.0/plugins/"></path>
    </ListOfPaths>
  </Configuration>

  <Handler>
      <InputHandler required="YES" input_classname="VLCGParseVarnerPBPKFlatFile" package="INPUT_HANDLER_PACKAGE"></InputHandler>
      <OutputHandler required="YES" output_classname="VLCGWriteJuliaPBPKModel" package="OUTPUT_HANDLER_PACKAGE"></OutputHandler>
  </Handler>
  <InputOptions>
      <NetworkFile required="YES" path_symbol="KWATEE_INPUT_PATH" filename="Model.net"></NetworkFile>
      <OrderFile required="NO" path_symbol="KWATEE_INPUT_PATH" filename="Order.dat"></OrderFile>
      <ModelParameterFile required="NO" path_symbol="KWATEE_INPUT_PATH" filename="Parameters.dat"></ModelParameterFile>
      <InitialConditionFile required="NO" path_symbol="KWATEE_INPUT_PATH" filename="InitialConditins.dat"></InitialConditionFile>
  </InputOptions>
  <OutputOptions>
      <DataFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="DataFile.jl"></DataFunction>
      <BalanceFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="MassBalances.jl"></BalanceFunction>
      <KineticsFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="Kinetics.jl"></KineticsFunction>
      <InputFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="Inputs.jl"></InputFunction>
      <DriverFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="SolveBalances.jl"></DriverFunction>
      <ControlFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="Control.jl"></ControlFunction>
      <FlowFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="Flow.jl"></FlowFunction>
      <DilutionFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="Dilution.jl"></DilutionFunction>
      <HeartRateFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="HeartRate.jl"></HeartRateFunction>
      <CardiacDistributionFunction required="YES" path_symbol="KWATEE_SOURCE_OUTPUT_PATH" filename="CardiacDistribution.jl"></CardiacDistributionFunction>
      <CompartmentConnectivityMatrix required="YES" path_symbol="KWATEE_NETWORK_OUTPUT_PATH" filename="Connectivity.dat"></CompartmentConnectivityMatrix>
      <StoichiometricMatrix required="YES" path_symbol="KWATEE_NETWORK_OUTPUT_PATH" filename="Network.dat"></StoichiometricMatrix>
      <DebugOutputFile required="YES" path_symbol="KWATEE_DEDUG_OUTPUT_PATH" filename="Debug.txt"></DebugOutputFile>
  </OutputOptions>
</Model>
~~~

The majority of the fields in the job configuration file can stay at the default values. However, you will need to specify your path structure (where Kwatee can find your files, where you want your generated code to reside, and where to find the server). Thus, you should edit the paths in the `<listOfPaths>...</listOfPaths>` section of the configuration file with your values. There typically only a few paths that must be specified:

* `KWATEE_INPUT_PATH`: Directory where Kwatee will find your `Model.net` file.
* `KWATEE_SOURCE_OUTPUT_PATH`: Directory where your generated model source code will be written (default is `src`).
* `KWATEE_NETWORK_OUTPUT_PATH`: Directory where your stoichiometric matrix be written (default is `network`).
* `KWATEE_DEBUG_OUTPUT_PATH`: Directory where any debug information is written (default is `debug`).
* `KWATEE_SERVER_ROOT_DIRECTORY`: Directory where your Kwatee server is installed.
* `KWATEE_SERVER_JAR_DIRECTORY`: Subdirectory where the Kwatee server jar file can be found (default is `dist`).
* `KWATEE_PLUGINS_JAR_DIRECTORY`: Subdirectory where Kwatee can find your plugin jars (default is `plugins`).
