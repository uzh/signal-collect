package com.signalcollect.configuration

import com.typesafe.config.ConfigFactory
import akka.event.Logging.LogLevel
import akka.event.Logging

object AkkaConfig {
  def get(akkaMessageCompression: Boolean, serializeMessages: Boolean, loggingLevel: LogLevel, kryoRegistrations: List[String]) = ConfigFactory.parseString(
    distributedConfig(akkaMessageCompression, serializeMessages, loggingLevel, kryoRegistrations))
  def distributedConfig(akkaMessageCompression: Boolean, serializeMessages: Boolean, loggingLevel: LogLevel, kryoRegistrations: List[String]) = """
akka {
  extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]

  # Event handlers to register at boot time (Logging$DefaultLogger logs to STDOUT)
  event-handlers = ["com.signalcollect.console.ConsoleLogger"]

  logConfigOnStart=on
    """ +
    {
      val level = loggingLevel match {
        case Logging.ErrorLevel   => "ERROR"
        case Logging.WarningLevel => "WARNING"
        case Logging.InfoLevel    => "INFO"
        case Logging.DebugLevel   => "DEBUG"
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

  actor {
    """ +
    {
      if (serializeMessages) {
        """
    serialize-messages = on
  """
      } else ""
    } +
    """
    provider = "akka.remote.RemoteActorRefProvider"

  	pinned-dispatcher {
	  type = PinnedDispatcher
	  executor = "thread-pool-executor"
  	}

    serializers {
      kryo = "com.romix.akka.serialization.kryo.KryoSerializer"
      java = "akka.serialization.JavaSerializer"
    }

    serialization-bindings {
      "scala.Int" = kryo
      "scala.Long" = kryo
      "scala.Float" = kryo
      "scala.Double" = kryo
      "scala.Some" = kryo
      "java.util.HashMap" = kryo
      "com.signalcollect.interfaces.EdgeId" = kryo
      "com.signalcollect.interfaces.SignalMessage" = kryo
      "com.signalcollect.interfaces.BulkSignal" = kryo
      "com.signalcollect.interfaces.WorkerStatus" = kryo
      "com.signalcollect.interfaces.NodeStatus" = kryo
      "com.signalcollect.interfaces.Heartbeat" = kryo
      "com.signalcollect.interfaces.WorkerStatistics" = kryo
      "com.signalcollect.interfaces.NodeStatistics" = kryo
      "com.signalcollect.interfaces.SentMessagesStats" = kryo
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

    deployment {

      default {

        # if this is set to a valid remote address, the named actor will be deployed
        # at that node e.g. "akka://sys@host:port"
        remote = ""

        target {

          # A list of hostnames and ports for instantiating the children of a
          # non-direct router
          #   The format should be on "akka://sys@host:port", where:
          #    - sys is the remote actor system name
          #    - hostname can be either hostname or IP address the remote actor
          #      should connect to
          #    - port should be the port for the remote server on the other node
          # The number of actor instances to be spawned is still taken from the
          # nr-of-instances setting as for local routers; the instances will be
          # distributed round-robin among the given nodes.
          nodes = []

        }
      }
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

        idstrategy = "incremental"

        # Define a default size for serializer pool
        # Try to define the size to be at least as big as the max possible number
        # of threads that may be used for serialization, i.e. max number
        # of threads allowed for the scheduler
        serializer-pool-size = 24

        # Define a default size for byte buffers used during serialization
        buffer-size = 4096

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
            "scala.Int" = 26
            "scala.Long" = 27
            "scala.Float" = 28
            "scala.Double" = 29
            "scala.Some" = 30
            "com.signalcollect.interfaces.SignalMessage" = 31
            "com.signalcollect.interfaces.BulkSignal" = 32
            "java.util.HashMap" = 33
            "com.signalcollect.interfaces.EdgeId" = 34
            "com.signalcollect.interfaces.WorkerStatus" = 35
            "com.signalcollect.interfaces.NodeStatus" = 36
            "com.signalcollect.interfaces.Heartbeat" = 37
            "com.signalcollect.interfaces.WorkerStatistics" = 38
            "com.signalcollect.interfaces.NodeStatistics" = 39
            "com.signalcollect.interfaces.SentMessagesStats" = 40
    """ +
    {
      if (!kryoRegistrations.isEmpty) {
        var highestUsedKryoId = 40
        var bindingsBlock = kryoRegistrations map { kryoRegistration =>
          highestUsedKryoId += 1
          s"""
             "$kryoRegistration" = $highestUsedKryoId"""
        }
        bindingsBlock.foldLeft("")(_ + _)
      } else {
        ""
      }
    } +
    """
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
            "scala.Int",
            "scala.Long",
            "scala.Float",
            "scala.Double",
            "scala.Some",
            "com.signalcollect.interfaces.SignalMessage",
            "com.signalcollect.interfaces.BulkSignal",
            "java.util.HashMap",
            "com.signalcollect.interfaces.EdgeId",
            "com.signalcollect.interfaces.WorkerStatus",
            "com.signalcollect.interfaces.NodeStatus",
            "com.signalcollect.interfaces.Heartbeat",
            "com.signalcollect.interfaces.WorkerStatistics",
            "com.signalcollect.interfaces.NodeStatistics",
            "com.signalcollect.interfaces.SentMessagesStats"
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
    """ +
    {
      if (akkaMessageCompression) {
        """
      # Options: "zlib" (lzf to come), leave out for no compression
      compression-scheme = "zlib"
      """
      } else ""
    } +
    """
    # Options: 0-9 (1 being fastest and 9 being the most compressed), default is 6
    # zlib-compression-level = 9

    # Which implementation of akka.remote.RemoteTransport to use
    # default is a TCP-based remote transport based on Netty
    transport = "akka.remote.netty.NettyRemoteTransport"

    # Enable untrusted mode for full security of server managed actors, allows
    # untrusted clients to connect.
    untrusted-mode = off

    # Timeout for ACK of cluster operations, lik checking actor out etc.
    remote-daemon-ack-timeout = 30s

    # If this is "on", Akka will log all inbound messages at DEBUG level, if off then they are not logged
    log-received-messages = off

    # If this is "on", Akka will log all outbound messages at DEBUG level, if off then they are not logged
    log-sent-messages = off

    # If this is "on", Akka will log all RemoteLifeCycleEvents at the level defined for each, if off then they are not logged
    log-remote-lifecycle-events = off

    # Each property is annotated with (I) or (O) or (I&O), where I stands for "inbound" and O for "outbound" connections.
    # The NettyRemoteTransport always starts the server role to allow inbound connections, and it starts
    # active client connections whenever sending to a destination which is not yet connected; if configured
    # it reuses inbound connections for replies, which is called a passive client connection (i.e. from server
    # to client).
    netty {

      # (O) In case of increased latency / overflow how long
      # should we wait (blocking the sender) until we deem the send to be cancelled?
      # 0 means "never backoff", any positive number will indicate time to block at most.
      backoff-timeout = 0ms

      # (I&O) Generate your own with '$AKKA_HOME/scripts/generate_config_with_secure_cookie.sh'
      #     or using 'akka.util.Crypt.generateSecureCookie'
      secure-cookie = ""

      # (I) Should the remote server require that it peers share the same secure-cookie
      # (defined in the 'remote' section)?
      require-cookie = off

      # (I) Reuse inbound connections for outbound messages
      use-passive-connections = off

      # (I) EXPERIMENTAL If "<id.of.dispatcher>" then the specified dispatcher
      # will be used to accept inbound connections, and perform IO. If "" then
      # dedicated threads will be used.
      use-dispatcher-for-io = ""

      # (I) The hostname or ip to bind the remoting to,
      # InetAddress.getLocalHost.getHostAddress is used if empty
      hostname = ""

      # (I) The default remote server port clients should connect to.
      # Default is 2552 (AKKA), use 0 if you want a random available port
      port = 0

      # (O) The address of a local network interface (IP Address) to bind to when creating
      # outbound connections. Set to "" or "auto" for automatic selection of local address.
      outbound-local-address = "auto"

      # (I&O) Increase this if you want to be able to send messages with large payloads
      message-frame-size = 1 MiB

      # (O) Timeout duration
      connection-timeout = 120s

      # (I) Sets the size of the connection backlog
      backlog = 8192

      # (I) Length in akka.time-unit how long core threads will be kept alive if idling
      execution-pool-keepalive = 60s

      # (I) Size of the core pool of the remote execution unit
      execution-pool-size = 4

      # (I) Maximum channel size, 0 for off
      max-channel-memory-size = 0b

      # (I) Maximum total size of all channels, 0 for off
      max-total-memory-size = 0b

      # (I&O) Sets the high water mark for the in and outbound sockets,
      # set to 0b for platform default
      write-buffer-high-water-mark = 0b

      # (I&O) Sets the low water mark for the in and outbound sockets,
      # set to 0b for platform default
      write-buffer-low-water-mark = 0b

      # (I&O) Sets the send buffer size of the Sockets,
      # set to 0b for platform default
      send-buffer-size = 0b

      # (I&O) Sets the receive buffer size of the Sockets,
      # set to 0b for platform default
      receive-buffer-size = 0b

      # (O) Time between reconnect attempts for active clients
      reconnect-delay = 5s

      # (O) Read inactivity period (lowest resolution is seconds)
      # after which active client connection is shutdown;
      # will be re-established in case of new communication requests.
      # A value of 0 will turn this feature off
      read-timeout = 0s

      # (O) Write inactivity period (lowest resolution is seconds)
      # after which a heartbeat is sent across the wire.
      # A value of 0 will turn this feature off
      write-timeout = 10s

      # (O) Inactivity period of both reads and writes (lowest resolution is seconds)
      # after which active client connection is shutdown;
      # will be re-established in case of new communication requests
      # A value of 0 will turn this feature off
      all-timeout = 0s

      # (O) Maximum time window that a client should try to reconnect for
      reconnection-time-window = 600s

      ssl {
        # (I&O) Enable SSL/TLS encryption.
        # This must be enabled on both the client and server to work.
        enable = off

        # (I) This is the Java Key Store used by the server connection
        key-store = "keystore"

        # This password is used for decrypting the key store
        key-store-password = "changeme"

        # (O) This is the Java Key Store used by the client connection
        trust-store = "truststore"

        # This password is used for decrypting the trust store
        trust-store-password = "changeme"

        # (I&O) Protocol to use for SSL encryption, choose from:
        # Java 6 & 7:
        #   'SSLv3', 'TLSv1'
        # Java 7:
        #   'TLSv1.1', 'TLSv1.2'
        protocol = "TLSv1"

        # Example: ["TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"]
        # You need to install the JCE Unlimited Strength Jurisdiction Policy
        # Files to use AES 256.
        # More info here: http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJCEProvider
        enabled-algorithms = ["TLS_RSA_WITH_AES_128_CBC_SHA"]

        # Using /dev/./urandom is only necessary when using SHA1PRNG on Linux to
        # prevent blocking. It is NOT as secure because it reuses the seed.
        # '' => defaults to /dev/random or whatever is set in java.security for
        #    example: securerandom.source=file:/dev/random
        # '/dev/./urandom' => NOT '/dev/urandom' as that doesn't work according
        #    to: http://bugs.sun.com/view_bug.do?bug_id=6202721
        sha1prng-random-source = ""

        # There are three options, in increasing order of security:
        # "" or SecureRandom => (default)
        # "SHA1PRNG" => Can be slow because of blocking issues on Linux
        # "AES128CounterSecureRNG" => fastest startup and based on AES encryption
        # algorithm
        # "AES256CounterSecureRNG"
        # The following use one of 3 possible seed sources, depending on
        # availability: /dev/random, random.org and SecureRandom (provided by Java)
        # "AES128CounterInetRNG"
        # "AES256CounterInetRNG" (Install JCE Unlimited Strength Jurisdiction
        # Policy Files first)
        # Setting a value here may require you to supply the appropriate cipher
        # suite (see enabled-algorithms section above)
        random-number-generator = ""
      }

    }

    # The dispatcher used for the system actor "network-event-sender"
    network-event-sender-dispatcher {
      executor = thread-pool-executor
      type = PinnedDispatcher
    }
  }
}
"""
}
