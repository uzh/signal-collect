# Signal/Collect

Signal/Collect is a programming model and framework for large-scale graph processing. The model is expressive enough to concisely formulate many iterated and data-flow algorithms on graphs, while allowing the framework to transparently parallelize the processing. The current release of the framework is not distributed yet.

Computations are executed on a graph, where the vertices are the computational units that interact by the means of signals that flow along the edges. This is similar to the actor programming model. All computations in the vertices are accomplished by collecting the incoming signals and then signaling the neighbors in the graph. You find more information about the programming model and features in the project wiki. 

Please visit the http://www.signalcollect.com for information about how to use Signal/Collect.