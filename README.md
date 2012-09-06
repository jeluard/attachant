![CI status](https://secure.travis-ci.org/jeluard/attachant.png)

Programmatically attach Java agents to local HotSpot virtual machines.

# Usage

```
Usage: java -jar attachant.jar id [options]

where options include:
  load agentJarPath (options)					To load specified agent jar
  load-self (options)						To load this jar as an agent
  load-remote-management port authenticate ssl (options)        To load remote-management agent
```

# Maven dependency

```xml
<dependency>
  <groupId>com.github.jeluard</groupId>
  <artifactId>attachant</artifactId>
  <version>0.9</version>
</dependency>
```

Released under [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0.html).