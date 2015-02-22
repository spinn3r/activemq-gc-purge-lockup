# Overview

Test project which demonstrates a flaw with ActiveMQ 5.10 whereby large
numbers of queue will cause producers/consumers to block for long periods of
time.  Up to one 1-15 minutes in production.

# To run

Just run:

```
export MAVEN_OPTS="-Xmx1400m -XX:MaxPermSize=384m"
mvn test
```

It will then log while queues are being created, then at the end it will print
latencies for regular / existing consumers vs creating a NEW consumer.  The
new consumer will block for a long period of time.