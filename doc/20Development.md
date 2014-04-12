## Developing MONET

It is relatively simple to develop for MONET. The source Code is
distributed using git and can be cloned from Github (in the near
future). The coding style is pretty much as described in the coding
conventions from Sun/Oracle with the exception that we use tabs to
indent the source-code. MONET is organized with
[Maven](http://maven.apache.org/), so just google for the phrase
"import maven project <your IDE here>" and you should be able to work
on the source quickly.

### Project structure

The platform itself is divided into two packages, the `controlserver`
and the `worker`. A worker is a java program that tries to connect to
a controlserver and requests jobs, i.e. Algorithms, to execute and
finally wirtes the results of the job into the data base. The
controlserver manages an arbitrary amount of workers and provides a
web frontend to create experiments and upload algorithms, parsers,
graphs and arbitrary [OSGi](http://www.osgi.org/) bundles. If you want
to create algorithms (or bundles in general) and you already have a
running controlserver at your disposal, you can skip the rest of this
page and navigate to the appropriate page from the documentation's
navigation bar.

### Setting up mongodb

MONET requires a [mongoDB](http://www.mongodb.org/) instance to
work. On a UNIX-based operating system, just install mongodb via your
package-manager or whatever you use to install software and you can
run a simple mongodb instance with the following command line:

> dbpath="/tmp/mongo" ; [ -d "$dbpath" ] || mkdir -p "$dbpath" ;
> mongod --dbpath "$dbpath" --smallfiles ; unset dbpath

You can of course change the path for the database files to any
location you have write access instead of `/tmp/mongo`.

### Running the control server

There are a few ways to start a controlserver instance. The
recommended way is to use an application server from within the
IDE. There is enough documentation around to set up an application
server in various IDEs. Eclipse, for example, has quite a convenient
way to run a [JBoss](http://www.jboss.org ) application server, just
to mention one possibility here.

There is also the test class `Start` which runs an embedded jetty
session and accepts various command line parameters. A noteworthy one
is `-w` which starts a worker that connects to the controlserver
automatically. For all the other parameters, see the deployment
section which can be reached from the documentation's navigation
bar. The default values should suffice for a relatively convenient way
to develop MONET.

Another possibility is to run the maven goal `jetty:run` which also
starts an embedded jetty server, but is configurable like a production
server. See the deployment chapter's section about configuring MONET
using Maven profiles.

### Running a worker

Running a worker is very simple. You either create a jar via

> mvn package

or your run the `WorkerMain`'s `main`-method and supply the required
command-line parameters. It is recommended to use a configuration file
and supply the command line paramater `-c /path/to/your/configuration`
to the `main`-method. For details about the configuration options, see
the deployment section of the documentation page.

### Writing or editing this documentation

This documentation is stored in a simple way. The controlserver has a
configuration option for the documentation's path and any file ending
with `.md` will be included in the navigation bar. There are only a
few simple rules to follow otherwise.

* The first to characters of the file name are used to sort the
  elements in the navigation bar in lexicographical order.

* Everything between the first two characters and the file ending
  `.md` will be used as is for the navigation bar's link name.

* The documentation is updated each time you click on an item in the
  navigation bar (but only if something changed).

* Subdirectories are not supported.
