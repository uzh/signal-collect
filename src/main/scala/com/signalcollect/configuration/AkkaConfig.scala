package com.signalcollect.configuration

import com.typesafe.config.ConfigFactory
import akka.event.Logging.LogLevel
import akka.event.Logging
import java.net.InetAddress

object AkkaConfig {
  def get(
    serializeMessages: Boolean,
    loggingLevel: LogLevel,
    kryoRegistrations: List[String],
    kryoInitializer: String,
    hostname: String = InetAddress.getLocalHost.getHostAddress,
    port: Int = 0,
    numberOfCores: Int = Runtime.getRuntime.availableProcessors) = ConfigFactory.parseString(
    distributedConfig(
      serializeMessages,
      loggingLevel,
      kryoRegistrations,
      kryoInitializer,
      hostname,
      port,
      numberOfCores))
  def distributedConfig(
    serializeMessages: Boolean,
    loggingLevel: LogLevel,
    kryoRegistrations: List[String],
    kryoInitializer: String,
    hostname: String,
    port: Int,
    numberOfCores: Int) = """
akka {
      
  extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
      
  # Event handlers to register at boot time (Logging$DefaultLogger logs to STDOUT)
  loggers = ["akka.event.Logging$DefaultLogger", "com.signalcollect.console.ConsoleLogger"]

  # Possibility to turn off logging of dead letters while the actor system
  # is shutting down. Logging is only done when enabled by 'log-dead-letters'
  # setting.
  log-dead-letters-during-shutdown = off

    """ +
    {
      val level = loggingLevel match {
        case Logging.ErrorLevel => "ERROR"
        case Logging.WarningLevel => "WARNING"
        case Logging.InfoLevel => "INFO"
        case Logging.DebugLevel => "DEBUG"
      }
      s"""
  loglevel = $level
  """
    } +
    """
  # debug {
    # enable function of LoggingReceive, which is to log any received message at
    # DEBUG level
    # receive = on
    # log-config-on-start = on
    # lifecycle = on
    # log-sent-messages = on
    # log-received-messages = on
  # }

  scheduler.tick-duration = 2ms
    
  actor {
    """ + {
      if (serializeMessages) {
        """
    serialize-messages = on
  """
      } else ""
    } + """
    provider = "akka.remote.RemoteActorRefProvider"
    
    serializers {
      kryo = "com.romix.akka.serialization.kryo.KryoSerializer"
    }
    
    serialization-bindings {
      "java.io.Serializable" = none
      "java.lang.Throwable" = java
      "akka.event.Logging$Error" = java
      "java.lang.Integer" = kryo
      "java.lang.Long" = kryo
      "java.lang.Float" = kryo
      "java.lang.Double" = kryo
      "java.lang.Boolean" = kryo
      "java.lang.Short" = kryo
      "scala.Tuple2" = kryo
      "scala.Tuple3" = kryo
      "scala.Tuple4" = kryo
      "scala.Tuple5" = kryo
      "scala.Tuple6" = kryo
      "scala.Tuple7" = kryo
      "scala.Tuple8" = kryo
      "scala.Tuple9" = kryo
      "scala.Tuple10" = kryo
      "scala.Tuple11" = kryo
      "scala.Tuple12" = kryo
      "scala.collection.BitSet" = kryo
      "scala.collection.SortedSet" = kryo
      "scala.util.Left" = kryo
      "scala.util.Right" = kryo
      "scala.collection.SortedMap" = kryo
      "akka.actor.SystemGuardian$RegisterTerminationHook$" = kryo
      "akka.actor.ReceiveTimeout$" = kryo
      "scala.Int" = kryo
      "scala.Long" = kryo
      "scala.Float" = kryo
      "scala.Double" = kryo
      "scala.Boolean" = kryo
      "scala.Short" = kryo
      "java.lang.String" = kryo
      "scala.Option" = kryo
      "scala.collection.immutable.Map" = kryo
      "scala.collection.Traversable" = kryo
      "[B" = kryo
      "[I" = kryo
      "[D" = kryo
      "[J" = kryo
      "[Ljava.lang.String;" = kryo
      "[[B" = kryo
      "[[I" = kryo
      "[[D" = kryo
      "[[J" = kryo
      "[[Ljava.lang.String;" = kryo
      "java.util.HashMap" = kryo
      "com.signalcollect.interfaces.EdgeId" = kryo
      "com.signalcollect.interfaces.SignalMessageWithSourceId" = kryo
      "com.signalcollect.interfaces.SignalMessageWithoutSourceId" = kryo
      "com.signalcollect.interfaces.BulkSignal" = kryo
      "com.signalcollect.interfaces.BulkSignalNoSourceIds" = kryo
      "com.signalcollect.interfaces.BulkStatus" = kryo
      "com.signalcollect.interfaces.WorkerStatus" = kryo
      "com.signalcollect.interfaces.NodeStatus" = kryo
      "com.signalcollect.interfaces.WorkerStatistics" = kryo
      "com.signalcollect.interfaces.NodeStatistics" = kryo
      "com.signalcollect.interfaces.SentMessagesStats" = kryo
      "com.signalcollect.interfaces.AddVertex" = kryo
      "com.signalcollect.interfaces.AddEdge" = kryo
      "com.signalcollect.interfaces.Request" = kryo
      "com.signalcollect.coordinator.OnIdle" = kryo
      "com.signalcollect.node.IdleReportRequested" = kryo
      "com.signalcollect.worker.StatsDue$" = kryo
      "com.signalcollect.worker.ScheduleOperations$" = kryo
      "com.signalcollect.worker.Ping" = kryo
      "com.signalcollect.worker.Pong" = kryo
      "com.signalcollect.worker.StartPingPongExchange" = kryo
      "akka.actor.Terminated" = kryo
      "akka.actor.SystemGuardian$TerminationHookDone$" = kryo
      "akka.actor.StopChild" = kryo
      "akka.remote.RemoteWatcher$HeartbeatTick$" = java
      "akka.remote.RemoteWatcher$ReapUnreachableTick$" = java
      "akka.dispatch.sysmsg.Terminate" = java
      "akka.actor.SystemGuardian$TerminationHook$" = java
      "scala.runtime.BoxedUnit" = java
      "akka.actor.PoisonPill$" = java
      "akka.actor.Identify" = java
      "akka.actor.ActorRef" = java
      "akka.actor.ActorIdentity" = java
    """ +
    {
      if (!kryoRegistrations.isEmpty) {
        var bindingsBlock = kryoRegistrations filter (!_.startsWith("Array")) map { kryoRegistration =>
          s"""
             "$kryoRegistration" = kryo"""
        }
        bindingsBlock.foldLeft("")(_ + _)
      } else {
        ""
      }
    } +
    """
    }

    kryo  {
        # Possibles values for type are: graph or nograph
        # graph supports serialization of object graphs with shared nodes
        # and cyclic references, but this comes at the expense of a small overhead
        # nograph does not support object grpahs with shared nodes, but is usually faster
        type = "nograph"

        # Possible values for idstrategy are:
        # default, explicit, incremental
        #
        # default - slowest and produces bigger serialized representation. Contains fully-
        # qualified class names (FQCNs) for each class
        #
        # explicit - fast and produces compact serialized representation. Requires that all
        # classes that will be serialized are pre-registered using the "mappings" and "classes"
        # sections. To guarantee that both sender and receiver use the same numeric ids for the same
        # classes it is advised to provide exactly the same entries in the "mappings" section
        #
        # incremental - fast and produces compact serialized representation. Support optional
        # pre-registering of classes using the "mappings" and "classes" sections. If class is
        # not pre-registered, it will be registered dynamically by picking a next available id
        # To guarantee that both sender and receiver use the same numeric ids for the same
        # classes it is advised to pre-register them using at least the "classes" section

        idstrategy = "explicit"

        # Define a default size for serializer pool
        # Try to define the size to be at least as big as the max possible number
        # of threads that may be used for serialization, i.e. max number
        # of threads allowed for the scheduler
        serializer-pool-size = """ + {2 * numberOfCores} + """

        # Define a default size for byte buffers used during serialization
        buffer-size = 65536

        # If set, akka uses manifests to put a class name
        # of the top-level object into each message
        use-manifests = false

        # Log implicitly registered classes. Useful, if you want to know all classes
        # which are serialized. You can then use this information in the mappings and/or
        # classes sections
        implicit-registration-logging = true

        # If enabled, Kryo logs a lot of information about serialization process.
        # Useful for debugging and lowl-level tweaking
        kryo-trace = false

        # If proviced, Kryo uses the class specified by a fully qualified class name
        # to perform a custom initialization of Kryo instances in addition to what
        # is done automatically based on the config file.
        kryo-custom-serializer-init = """" + kryoInitializer + """"
    
        kryo-reference-map = false

        # Define mappings from a fully qualified class name to a numeric id.
        # Smaller ids lead to smaller sizes of serialized representations.
        #
        # This section is mandatory for idstrategy=explicit
        # This section is optional  for idstrategy=incremental
        # This section is ignored   for idstrategy=default
        #
        # The smallest possible id should start at 20 (or even higher), because
        # ids below it are used by Kryo internally e.g. for built-in Java and
        # Scala types
        mappings {
        }

        # Define a set of fully qualified class names for
        # classes to be used for serialization.
        # The ids for those classes will be assigned automatically,
        # but respecting the order of declaration in this section
        #
        # This section is optional  for idstrategy=incremental
        # This section is ignored   for idstrategy=default
        # This section is optional  for idstrategy=explicit
        classes = [
            "com.signalcollect.examples.PageRankEdge",
            "com.signalcollect.examples.PageRankVertex"
    """ +
    {
      if (!kryoRegistrations.isEmpty) {
        var bindingsBlock = kryoRegistrations filter (!_.startsWith("Array")) map { kryoRegistration =>
          s""",
             "$kryoRegistration""""
        }
        bindingsBlock.foldLeft("")(_ + _)
      } else {
        ""
      }
    } +
    """
        ]
    }
  }

  remote {
    
    log-remote-lifecycle-events = """ + { if (loggingLevel.asInt > 2) "on" else "off" } + """

    ### Failure detection and recovery
 
    # Settings for the Phi accrual failure detector (http://ddg.jaist.ac.jp/pub/HDY+04.pdf
    # [Hayashibara et al]) used by the remoting subsystem to detect failed
    # connections.
    transport-failure-detector {
 
      # FQCN of the failure detector implementation.
      # It must implement akka.remote.FailureDetector and have
      # a public constructor with a com.typesafe.config.Config and
      # akka.actor.EventStream parameter.
      implementation-class = "akka.remote.PhiAccrualFailureDetector"
 
      # How often keep-alive heartbeat messages should be sent to each connection.
      heartbeat-interval = 30 s
 
      # Defines the failure detector threshold.
      # A low threshold is prone to generate many wrong suspicions but ensures
      # a quick detection in the event of a real crash. Conversely, a high
      # threshold generates fewer mistakes but needs more time to detect
      # actual crashes.
      threshold = 14.0
 
      # Number of the samples of inter-heartbeat arrival times to adaptively
      # calculate the failure timeout for connections.
      max-sample-size = 100
 
      # Minimum standard deviation to use for the normal distribution in
      # AccrualFailureDetector. Too low standard deviation might result in
      # too much sensitivity for sudden, but normal, deviations in heartbeat
      # inter arrival times.
      min-std-deviation = 100 ms
 
      # Number of potentially lost/delayed heartbeats that will be
      # accepted before considering it to be an anomaly.
      # This margin is important to be able to survive sudden, occasional,
      # pauses in heartbeat arrivals, due to for example garbage collect or
      # network drop.
      acceptable-heartbeat-pause = 30 s
    }
 
    # Settings for the Phi accrual failure detector (http://ddg.jaist.ac.jp/pub/HDY+04.pdf
    # [Hayashibara et al]) used for remote death watch.
    watch-failure-detector {
 
      # FQCN of the failure detector implementation.
      # It must implement akka.remote.FailureDetector and have
      # a public constructor with a com.typesafe.config.Config and
      # akka.actor.EventStream parameter.
      implementation-class = "akka.remote.PhiAccrualFailureDetector"
 
      # How often keep-alive heartbeat messages should be sent to each connection.
      heartbeat-interval = 1 s
 
      # Defines the failure detector threshold.
      # A low threshold is prone to generate many wrong suspicions but ensures
      # a quick detection in the event of a real crash. Conversely, a high
      # threshold generates fewer mistakes but needs more time to detect
      # actual crashes.
      threshold = 14.0
 
      # Number of the samples of inter-heartbeat arrival times to adaptively
      # calculate the failure timeout for connections.
      max-sample-size = 200
 
      # Minimum standard deviation to use for the normal distribution in
      # AccrualFailureDetector. Too low standard deviation might result in
      # too much sensitivity for sudden, but normal, deviations in heartbeat
      # inter arrival times.
      min-std-deviation = 100 ms
 
      # Number of potentially lost/delayed heartbeats that will be
      # accepted before considering it to be an anomaly.
      # This margin is important to be able to survive sudden, occasional,
      # pauses in heartbeat arrivals, due to for example garbage collect or
      # network drop.
      acceptable-heartbeat-pause = 30 s
 
 
      # How often to check for nodes marked as unreachable by the failure
      # detector
      unreachable-nodes-reaper-interval = 1s
 
      # After the heartbeat request has been sent the first failure detection
      # will start after this period, even though no heartbeat mesage has
      # been received.
      expected-response-after = 3 s
 
    }
 
    # After failed to establish an outbound connection, the remoting will mark the
    # address as failed. This configuration option controls how much time should
    # be elapsed before reattempting a new connection. While the address is
    # gated, all messages sent to the address are delivered to dead-letters.
    # If this setting is 0, the remoting will always immediately reattempt
    # to establish a failed outbound connection and will buffer writes until
    # it succeeds.
    retry-gate-closed-for = 0 s
 
    # If the retry gate function is disabled (see retry-gate-closed-for) the
    # remoting subsystem will always attempt to reestablish failed outbound
    # connections. The settings below together control the maximum number of
    # reattempts in a given time window. The number of reattempts during
    # a window of "retry-window" will be maximum "maximum-retries-in-window".
    retry-window = 60 s
    maximum-retries-in-window = 3
 
    # The length of time to gate an address whose name lookup has failed
    # or has explicitly signalled that it will not accept connections
    # (remote system is shutting down or the requesting system is quarantined).
    # No connection attempts will be made to an address while it remains
    # gated. Any messages sent to a gated address will be directed to dead
    # letters instead. Name lookups are costly, and the time to recovery
    # is typically large, therefore this setting should be a value in the
    # order of seconds or minutes.
    gate-invalid-addresses-for = 60 s
 
    # This settings controls how long a system will be quarantined after
    # catastrophic communication failures that result in the loss of system
    # messages. Quarantining prevents communication with the remote system
    # of a given UID. This function can be disabled by setting the value
    # to "off".
    quarantine-systems-for = 60s
 
    # This setting defines the maximum number of unacknowledged system messages
    # allowed for a remote system. If this limit is reached the remote system is
    # declared to be dead and its UID marked as tainted.
    system-message-buffer-size = 1000
 
    # This setting defines the maximum idle time after an individual
    # acknowledgement for system messages is sent. System message delivery
    # is guaranteed by explicit acknowledgement messages. These acks are
    # piggybacked on ordinary traffic messages. If no traffic is detected
    # during the time period configured here, the remoting will send out
    # an individual ack.
    system-message-ack-piggyback-timeout = 1 s
 
    # This setting defines the time after messages that have not been
    # explicitly acknowledged or negatively acknowledged are resent.
    # Messages that were negatively acknowledged are always immediately
    # resent.
    resend-interval = 1 s

    # Log warning if the number of messages in the backoff buffer in the endpoint
    # writer exceeds this limit. It can be disabled by setting the value to off.
    log-buffer-size-exceeding = """ + { if (loggingLevel.asInt > 2) "100000" else "2000000" } + """
    
    netty.tcp {
        
      hostname = """" + hostname + """"

      # The default remote server port clients should connect to.
      # Default is 2552 (AKKA), use 0 if you want a random available port
      # This port needs to be unique for each actor system on the same machine.
      port = """ + port + """

      # Sets the send buffer size of the Sockets,
      # set to 0b for platform default
      send-buffer-size = 0b
 
      # Sets the receive buffer size of the Sockets,
      # set to 0b for platform default
      receive-buffer-size = 0b
 
      # Maximum message size the transport will accept, but at least
      # 32000 bytes.
      # Please note that UDP does not support arbitrary large datagrams,
      # so this setting has to be chosen carefully when using UDP.
      # Both send-buffer-size and receive-buffer-size settings has to
      # be adjusted to be able to buffer messages of maximum size.
      maximum-frame-size = 2097152b

      # (I) Sets the size of the connection backlog
      backlog = 8192
      
      # Used to configure the number of I/O worker threads on server sockets
      server-socket-worker-pool {
        # Min number of threads to cap factor-based number to
        pool-size-min = """ + numberOfCores + """
 
        # The pool size factor is used to determine thread pool size
        # using the following formula: ceil(available processors * factor).
        # Resulting size is then bounded by the pool-size-min and
        # pool-size-max values.
        #pool-size-factor = 1.0
 
        # Max number of threads to cap factor-based number to
        pool-size-max = """ + numberOfCores + """
      }
 
      # Used to configure the number of I/O worker threads on client sockets
      client-socket-worker-pool {
        # Min number of threads to cap factor-based number to
        pool-size-min = """ + numberOfCores + """
 
        # The pool size factor is used to determine thread pool size
        # using the following formula: ceil(available processors * factor).
        # Resulting size is then bounded by the pool-size-min and
        # pool-size-max values.
        #pool-size-factor = 1.0
 
        # Max number of threads to cap factor-based number to
        pool-size-max = """ + numberOfCores + """
      }
    }
      
    default-remote-dispatcher {
      # Dispatcher is the name of the event-based dispatcher
      type = Dispatcher
      # What kind of ExecutionService to use
      executor = "fork-join-executor"
      # Configuration for the fork join pool
      fork-join-executor {
        # Min number of threads to cap factor-based parallelism number to
        parallelism-min = """ + numberOfCores + """
        # Parallelism (threads) ... ceil(available processors * factor)
        #parallelism-factor = 1.0
        # Max number of threads to cap factor-based parallelism number to
        parallelism-max = """ + numberOfCores + """
      }
      # Throughput defines the maximum number of messages to be
      # processed per actor before the thread jumps to the next actor.
      # Set to 1 for as fair as possible.
      throughput = 1000
    }
    
    backoff-remote-dispatcher {
      # Dispatcher is the name of the event-based dispatcher
      type = Dispatcher
      # What kind of ExecutionService to use
      executor = "fork-join-executor"
      # Configuration for the fork join pool
      fork-join-executor {
        # Min number of threads to cap factor-based parallelism number to
        parallelism-min = """ + numberOfCores + """
        # Parallelism (threads) ... ceil(available processors * factor)
        #parallelism-factor = 1.0
        # Max number of threads to cap factor-based parallelism number to
        parallelism-max = """ + numberOfCores + """
      }
      # Throughput defines the maximum number of messages to be
      # processed per actor before the thread jumps to the next actor.
      # Set to 1 for as fair as possible.
      throughput = 1000
    }
        
  }
}
"""
}
