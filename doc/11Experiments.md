# Experiments

Experiments gather jobs that will be executed by a worker. An experiment consists of the following:

* One Algorithm that is exectued
* A various number of jobs (minimum one)

## List all experiments

If you click on the word *Experiments* in the bar, the list of created experiments is shown. An item of the list contains:

* The name,
* The actual state,
* The priority,
* The number of assigned jobs
* if at least one job is executed, the number of workers executing jobs is shown (minimum one)

## Creation of experiments

A new experiment is created by clicking on the *Add experiment* button. On the following page you have to configure the experiment, there you have to choose:

* a name
* a priority
* a description
* and the algorithm.

If you chose the algorithm, the parameters have to be set, they will appear bottom right. In general there is a short description for every parameter and the default is shown.

When each parameter is set and all information is given, you can create the experiment by clicking on the *Create experiment* button.

### Configure jobs to be executed

After creating the experiment you come to the detail view. There at the bottom of the side you can add jobs to the experiment. A job executes the algorithm specified by the experiment, it needs two additional parameter which can be set under *Add a job* :

* The *Graph file*
* The *Graph parser*

after chosing this a job is created by clicking on *Add job*.

### Assign Worker

It is possible to choose a particular worker to execute the jobs from an experiment. If a Worker is assigned, MONET only executes jobs on this worker. Multiple Workers could be assigned.

## Detail view of an experiment

If you click on of the experiments inside the list you come to the detail view. There you can show the results of an experiment that successful exectued it's jobs or you can add jobs to the experiment if it has not be executed yet.

### Show results

If the experiment is in the state successful you can show the results. MONET measures the running time and the Pareto-front during the execution of an experiment.

* Get running time: Click on the green button *Show results* at the top of the detail view of an experiment
* Get Pareto-front: Scroll inside the detail view downwards to the *Job list*. Under a successful finished job is a button *Show results*, by clicking on this button the Pareto-front will be shwon.

