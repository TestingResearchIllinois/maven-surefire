# Usage

This repository is a fork of Maven Surefire that contains two main modifications.

1. A Maven extension to ensure that any Maven project one runs ```mvn test``` on will use this custom version of Surefire instead, and
2. the ability to control the ordering of tests run directly with Maven Surefire.

## Setup

To use the plugin, please perform the following steps.

1. Run ```mvn install -DskipTests -Drat.skip``` in this directory
2. Copy ```surefire-changing-maven-extension/target/surefire-changing-maven-extension-1.0-SNAPSHOT.jar``` into your Maven installation's ```lib/ext``` directory. This [StackOverflow post](https://stackoverflow.com/a/39479104) may help indicate where your Maven installation is located

The copying of the extension helps ensure that any project you run ```mvn test``` on will now use this custom version of Surefire and change certain settings (e.g., reuseForks to false) to prevent issues with fixing the ordering of tests. More information on how to use Maven extensions can be found [here](https://maven.apache.org/examples/maven-3-lifecycle-extensions.html#use-your-extension-in-your-build-s). Note that if you already have ```surefire-changing-maven-extension-1.0-SNAPSHOT.jar``` in your Maven installation's ```lib/ext``` you must first remove the jar before installing again.

## Example

```
mvn test -Dsurefire.runOrder=testorder \
-Dtest=org.apache.dubbo.rpc.protocol.dubbo.DubboLazyConnectTest#testSticky1,\
org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest#testDubboProtocol,\
org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest#testDubboProtocolWithMina \
-pl dubbo-rpc/dubbo-rpc-dubbo
```

By specifying ```-Dsurefire.runOrder=testorder``` Maven test will run the specifed tests in the order that they appear in ```-Dtest```.
Specifically, running the command above will result in the tests running in the following order:

1. org.apache.dubbo.rpc.protocol.dubbo.DubboLazyConnectTest.testSticky1
2. org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest.testDubboProtocol
3. org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest.testDubboProtocolWithMina

## Example with file

```
mvn test -Dsurefire.includesFile=path_to_file -Dsurefire.runOrder=testorder -pl dubbo-rpc/dubbo-rpc-dubbo
```

By specifying ```-Dsurefire.runOrder=testorder``` Maven test will run the specifed tests in the order that they appear in the file ```path_to_file```. Note that the ```path_to_file``` should be an **absolute** path (e.g., ```/home/user/project/test-list```).

Assume the content of ```path_to_file``` are the following:

```
org.apache.dubbo.rpc.protocol.dubbo.DubboLazyConnectTest#testSticky1
org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest#testDubboProtocol
org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest#testDubboProtocolWithMina
```

Then running the Maven command above will result in the tests running in the following order:

1. org.apache.dubbo.rpc.protocol.dubbo.DubboLazyConnectTest.testSticky1
2. org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest.testDubboProtocol
3. org.apache.dubbo.rpc.protocol.dubbo.DubboProtocolTest.testDubboProtocolWithMina


## Random with seed

This specific feature has been merged to [apache/maven-surefire](https://github.com/apache/maven-surefire/pull/309). The other features in this repository will be submitted to Surefire soon.

Specifically, Surefire will:

1. Output of the random seed used to generate a particular random test order when `-Dsurefire.runOrder.random.seed` and `-Dfailsafe.runOrder.random.seed` are not set. To get the seed, look for the following in the output:
```
Tests will run in random order. To reproduce ordering use flag -Dsurefire.runOrder.random.seed=28421961536740501
```
2. Replay a previously observed random test order by setting `-Dsurefire.runOrder.random.seed` and `-Dfailsafe.runOrder.random.seed` to the seed that observed the random test order

Note that the seed will control the random of both test classes and test methods if both are set to random. E.g., ```-Dsurefire.runOrder=random -Dsurefire.runOrder.random.seed=28421961536740501``` will randomize both test classes and test methods with ```28421961536740501``` as the seed. It is currently not possible to set the seed of the test class and test method to be different.

Some tests were added to Surefire for this feature. These tests ensure that the setting of the same random seeds do create the same test orders and different random seeds do create different test orders. Note that the inherent randomness of the orders does mean that the tests can be flaky (nondeterministically pass or fail without changes to the code). The current tests have a rate of 0.4% (1/3)^5 of failing. Increasing the number of tests (3) or the number of times to loop (5) would decrease the odds of the tests failing.

## Caveats

1. **Test methods from different test classes cannot interleave.**  When test methods from various test classes interleave, all test methods from the first time the test class runs will run then as well. E.g., If tests ClassA.testA, ClassB.testA, ClassA.testB are provided, then the run order will be ClassA.testA, ClassA.testB, ClassB.testA.

2. **FixMethodOrder annotations are ignored.** JUnit 4.11+ provides the annotation [FixMethodOrder](https://junit.org/junit4/javadoc/4.12/org/junit/FixMethodOrder.html) to control the ordering in which tests are run. Such annotations are ignored when this plugin is used. E.g., If tests ClassA.testB, ClassA.testA are provided and FixMethodOrder is set to NAME_ASCENDING, then the run order will still be ClassA.testB, ClassA.testA.

## TODOs

The following are features that we would like to have but are yet to be supported.

1. Have surefire reports save the order in which the test classes are run
2. Allow one to get just the test order list without running tests
3. Reverse mode
4. Have Surefire fix order-dependent tests observed in last run
