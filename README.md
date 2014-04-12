# MONET

MONET - Multi Objective NETwork optimization platform

This repository contains two Java projects which build the essence of
the platform: the controlserver and the worker.  Keep in mind that
this is a prototype and still needs some work to be used in a proper
fashion.

MONET itself is a platform that allows comparison of algorithms
(currently only in Java).

# Documentation

Most of the documentation is in the doc folder as markdown files with
a number prefix. They are used by the running platform itself and is
therefore not put into this document.

# License

This platform (as well as some algorithms, parsers and a graph
representation) have been created in a work group which is part of the
studies at a university to complete the Master of Science degree at
the "Technische Universität Dortmund". We decided to release this
project under the GNU AFFERO GENERAL PUBLIC LICENSE (Version 3, 19
November 2007). Some interfaces are put under the GNU LESSER PUBLIC
LICENSE (Version 3, 29 June 2007) so that you can even develop
algorithms to compare on this platform if you are not allowed to
disclose the source.

# Requirements

* http://www.mongodb.org/ - The database backend we chose for development.
* http://maven.apache.org/ - The build system that we chose.

## Controlserver

The Controlserver is a Java web application that can be deployed on
application servers. As of now it was only tested with the JBoss
appliation server, but as we did not use any JBoss specific libraries
or tools, it shoul run on other application servers as well.

## Worker

Simple Java program that runs OSGi bundles which conform to the
interfaces defined in the monet.interfaces package.

It is generally possible to write workers for other programming
languages as mainly the network communication must be reimplemented
which is relatively simple. And the control server must be taught to
distinguish between java packages and non-java packages.

# Future development

We are not interested in pushing this project into a production ready
state, mainly because we do not have any need for it. But if you plan
to do so, just tell me (woozletoff@gmail.com) and I will update this
README to link to your fork of the project.
