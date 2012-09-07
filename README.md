![CI status](https://secure.travis-ci.org/jeluard/attachant.png)

Programmatically attach [Java agents](http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html) to local HotSpot virtual machines.

# Usage

## Command line

```
Usage: java -jar attachant.jar id [options]

where options include:
  load agentJarPath (options)					To load specified agent jar
  load-self (options)						To load this jar as an agent
  load-remote-management port authenticate ssl (options)        To load remote-management agent
```

## API

```java
//To load an agent into a local VM
Agents.load("/path/to/agent.jar", pid, Optional.absent());

//To load this jar as an agent into a local VM
Agents.loadSelf(Agents.class, pid, Optional.absent());

//To load remote management agent
Agents.loadRemoteManagement(pid, 1234, false, false,  Optional.absent());
```

# Maven dependency

```xml
<dependency>
  <groupId>com.github.jeluard</groupId>
  <artifactId>attachant</artifactId>
  <version>0.9</version>
</dependency>
```

## Some agents

While Java agents are mostly needed at startup time some can be dynamically added to a running process. Here is a non-extensive list:

* [File leak detector](http://file-leak-detector.kohsuke.org/)
* [BTrace](http://kenai.com/projects/btrace/pages/UserGuide)
* [Byteman](https://www.jboss.org/byteman)
* [NewRelic](https://newrelic.com/docs/java/java-agent-installation)

Released under [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0.html).