# Development of an algorithm

An algorithm developed for MONET is an OSGi bundle packed with Maven. First you have to create an own project for the new algorithm, the folder structure could be as followd:

<pre>
+-myalgo/
|--src/
| +-my/private/package
| |--Activator.java
| |--MyAlgo.java
|--test/
|--ressources/
| |-- parameters.xml

</pre>

If you are familiar with Maven, the folder structures could be changed.

## Minimal Code

MONET requries a starting point which is the class that implements the *Algorithm* interface. The *Algorithm* interface consists just the execute Method

<pre><code class="java">
public void execute(Job job, Meter meter, ServiceDirectory serviceDir) throws Exception
</code></pre>

The parameters are:

* *job* contains the graph instance and the parameters to be set inside of the algorithm
* *meter* to save data to the specified data base
* *serviceDir* to load missing bundles

By implementing this, MONET can execute this bundle.

OSGi also needs an entry point, which is the class that implement the *BundleActivator* interface. That interface contains two Methods:

<pre><code class="java">
public void start(BundleContext context) throws Exception

public void stop(BundleContext context) throws Exception
</code></pre>

this methods are called when starting and ending this bundle. The Method <pre> <code class="java"> stop() </code></pre> did not need any special code. The new developed bundle has to be registered in the Service Directory, this is done by calling the method <pre><code class="java">ServiceDirectory.registerAlgorithm(context, new MyAlgo()) </code></pre>. The Parameter context is given by the *BundleActivator* and MyAlgo is the developed algorithm.

## pom.xml and parameters.xml

Maven needs the *pom.xml* to build the package. A simple one, where just a little has to be change for use is given below:

<pre>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>monet</groupId>
    <artifactId>monet</artifactId>
    <version>0.0.2-SNAPSHOT</version>
  </parent>
  <packaging>bundle</packaging>
  <artifactId><!-- Name hierhin --></artifactId>

  <dependencies>
    <dependency>
      <groupId>org.OSGi</groupId>
      <artifactId>org.OSGi.core</artifactId>
      <version>5.0.0</version>
      <scope>provided</scope>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>[4.0,)</version>
      <scope>compile</scope>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>monet</groupId>
      <artifactId>monet_worker</artifactId>
      <version>0.0.2-SNAPSHOT</version>
      <scope>compile</scope>
      <optional>false</optional>
    </dependency>

    <!-- spezifische Abhaengigkeiten -->
    <dependency>
    	<groupId>monet</groupId>
    	<artifactId>monet_graph</artifactId>
    	<version>0.0.2-SNAPSHOT</version>
        <type>bundle</type>
        <optional>true</optional>
    </dependency>
  </dependencies>
  <build>
    <sourceDirectory>src</sourceDirectory>
    <testSourceDirectory>tests</testSourceDirectory>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.0</version>
        <configuration>
          <source>1.7</source>
          <target>1.7</target>
        </configuration>
      </plugin>

      <plugin>
        <groupId>org.apache.felix</groupId>
        <artifactId>maven-bundle-plugin</artifactId>
        <version>2.3.7</version>
        <extensions>true</extensions>
        <configuration>
          <instructions>
            <Bundle-Name>${pom.name}</Bundle-Name>
            <Private-Package>
              <!-- Interne Pakete -->
            </Private-Package>
            <Bundle-Activator><!-- Klasse, welche BundleActivator implementiert --></Bundle-Activator>
            <Import-Package>
            <!-- Importierte Pakete -->
              org.OSGi.framework,
              monet.interfaces,
              monet.worker,
              org.apache.logging.log4j,
              monet.graph,
              monet.graph.interfaces,
              monet.graph.weighted,
            </Import-Package>
            <Export-Package>
              <!-- Exportierte Pakete -->
            </Export-Package>
            <Bundle-ClassPath>${maven-dependencies}</Bundle-ClassPath>
          </instructions>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
</pre>

Parameters used by the algorithm are set inside the Web GUI these are specified in the *parameters.xml* file. Each parameter consists of a name, a short description and a default value. You can specify an input field or a list to choose. An easy example following shows how a *parameters.xml* could look alike.

<pre>
<algorithmxmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="bundle-description.xsd">
    <name>SomeAlgorithm</name>
    <description>
        This algorithm is super awesome as it has a lot of parameters.
    </description>
    <parameters>
<parameter name="problemType">
  <description> The problem Type of the Algorithm </description>
  <choices>
    <choice value="SSSP">
      <parameter name="sssp_specific_parameter1">
        <description>sssp is awesome</description>
        <integer default="42"/>
      </parameter>
      <parameter name="sssp_specific_parameter2">
        <description>sssp is boring</description>
        <integer default="4711"/>}
      </parameter>
    </choice>
    <choice value="MST">
      <parameter name="mst_specific_parameter1">
        <description>mst is awesome</description>
        <decimal default="3.14159"/>
      </parameter>
    </choice>
  </choices>
</parameter>
 <parameter name="popsize">
            <description>
                Size of the population.
            </description>
            <integer default="1"/>
        </parameter>
</parameters>
</algorithm>
</pre>
