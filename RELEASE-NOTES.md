Release 4.0.0 changes relative to 3.0.3
This release is motivated by features that are useful for TripleRush, it is only a major release because it breaks an existing API.  
- API change: Now passing a graph editor to `EdgeAddedToNonExistentVertexHandler` (https://github.com/uzh/signal-collect/issues/160)
- Changed `GraphEditor.loadGraph` so it also works when the computation is executing (https://github.com/uzh/signal-collect/issues/161)
