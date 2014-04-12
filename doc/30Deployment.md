## Deploying MONET

This page describes the MONET specific steps to deploy the web
frontend or the worker which runs the algorithms. The first step is to
set up a database. MONET uses [mongoDB](http://www.mongodb.org/) as
its data base backend. Configure it as you like, but keep in mind that
it will have impact on the configuration file options which are
described after possible deployment options are presented.

### Deploying the web frontend

There are a few different possibilities to configure a worker or
controlserver. To deploy a controlserver you can use
[Maven profiles](http://maven.apache.org/guides/introduction/introduction-to-profiles.html)
to configure the generated war at compile time (which is quite
convenient).  There is a _production_ template defined in the
`pom.xml` of the controlserver project. It should be straight-forward
to adjust it to your setup.

Alternatively you can edit the `monetrc` file in the
`src/main/resources` folder of the controlserver-project manually and
then issue the appropriate maven goal to deploy or build the
controlserver's war file. The provided `pom.xml` file includes a
template for the
[JBoss-AS-Maven-plugin](https://docs.jboss.org/jbossas/7/plugins/maven/latest/)
which is also not very difficult to use. If you chose this method, you
can deploy MONET with simple command line like this:

> mvn jboss-as:deploy

Feel free to use whatever you like.

### Deploying a worker

A worker is deployed by simply executing a jar file and provide the
appropriate command line option(s).  The worker's jar can be build
using the Maven goal `package` in which a statically linked jar will
be generated that can simply be run by issueing the following command:

> java -jar worker*with-dependencies.jar -c /path/to/config/file

You can also squeeze JVM parameters after `java` to increase the heap
or stack size, for example. You can also supply every configuration
option as an equally named command-line option preceeded with two dashes.

### The configuration file

The configuration file is a simple java properties file
(i.e. _key=value_) which has the following options:

* __dbusername__ This is simply the username you need to connect to
  the data base. It is unset by default.

* __dbpassword__ The password to use for the given _dbusername_. This
  value is unset as well by default.

* __dbname__ The name of the data base collection to store all the
  bundles, graphs and experiment results in. This is _monet_ by
  default.

* __dbhost__ The hostname of the database server. For obvious reasons,
  this is _localhost_ by default.

* __dbport__ The port to the database. This is equivalent to mongodb's
  default of _27017_.

* __host__ This is the host name of the control server. This setting
  is obviously only necessary for the worker and its default value is
  _localhost_.

* __controlport__ Due to historical reasons, the name of this
  parameter is unecessarily long as the port, to which a worker should
  connect is only reasonable to be the control server's. Also, it is
  quite probable that the workers are behind Firewalls or NATs and
  cannot be reached from the controlserver anyway. The default for
  this is _33380_ (for no good reason).

* __cache__ This is a directory in which bundles, graphs etc. are
  stored. This is mainly used by workers but also important to set for
  the controlserver. The default value for this is the present working
  directory.

* __documentation__ The dynamically loaded documentation's directory
  is described with this option. By default it is the _doc_-directory
  in the _cache_ directory.
