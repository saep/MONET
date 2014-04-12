# Namoa
The Namoa algorithm for solving the single source single direction shortest path problem.
The algorithm doesn't need any parameters. It has to be given an heuristic, that for each node estimates the remaining distance to the direction of the shortest path problem. This heuristic can be provided in several ways:

* If no heuristic is passed by the user, the algorihm will calculate an heuristic based on the input graph, which will obviously not be very accurate.
* The monet_heur_parser can be used to parse an heuristic while parsing the input graph. In this case the input file has to contain the graph in the MONET graph format followed by a line containing 'START_HEURISTIC', which itself is then followed by the heuristic itself. Here each line starts with the number of the node whose heuristic value is given, followed by the vector that is to be set as the heuristic.
