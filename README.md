## Need for event driven architecture

Drawback of sync communication:
- One service performance might affect another.
- Downstream services need to be aware of upstream services, and the request/response contract, and maybe scale accordingly.
- Not flexible (Introducing new services in the workflow)
- Upstream services have to be available.
- We might lost the request incase of 500 error.
- EDM is an architectural pattern which focuses on loosely coupled and async communication among microservices.
    - We use some event bus like Apache Kafka through which we publish event or messages.
    - We will be setting up Kafka cluster with multiple Kafka brokers and we will be having Kafka producers and consumers.

Note: 
- In Kafka KRaft, we do not need the ZooKeeper, one of the Kafka servers in Kafka cluster would be acting like the manager for Kafka.
- When we start the Kafka server, we have to provide the path where server.properties is located. Based on these properties, Kafka server will behave accordingly.

## Docker Image Used

We will be using docker to spin up kafka server.
- https://github.com/vinsguru/reactive-kafka-course/blob/master/01-workspace/01-kafka-setup/image/Dockerfile
- https://github.com/vinsguru/reactive-kafka-course/blob/master/01-workspace/01-kafka-setup/image/runner.sh

## Introduction

- Kafka is an event streaming platform.
- It captures any events in real time and store them for later retrieval/processing.
- Kafka uses topic to organise data.
- What would happen if a server (in which there is a topic from which consumer is reading) inside Kafka cluster is down? Answered it later.

## Kafka Cluster

- In real life multiple instance of Kafka server or broker or node would be running.
- There will be a controller among these brokers which would assign some responsibilities to the brokers i.e. manages the cluster. What if controller dies? Another controller from remaining brokers would be selected.
- When we want to create a topic, controller will assign one broker/node to own that topic. What if that broker crashes? Controller will identify some nodes/brokers to be backup for that topic/broker in general. When main node dies then one of the other nodes will become primary.
- A single broker can manage/be primary for multiple topics.
- Leader and follower terminology are used instead of primary and backup, example some broker (read and write) server could be leader for order_event topic and few other broker would be followers for that topic.

### Bootstrap Server

- Bootstrap-servers: Set of servers (out of N number of Kafka servers) to provide initial metadata.
- Kafka client library would interact with bootstrap-servers (servers in Kafka cluster which know that which node/broker is leader/follower of what topic) then with that info the KCL will interact with the leader server of the topic to publish/read data.

Note:
- Kafka stores event in binary format.

## Useful Commands

- Exec into docker container
```
docker exec -it <container-name> <command>
docker exec -it
```

- List kafka topic, delete, create, describe and create with partitions
```
kafka-topics.sh --bootstrap-server localhost:9092 --list
kafka-topics.sh --bootstrap-server localhost:9092 --topic  --delete
kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --delete
kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --create
kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --describe
kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --create --partitions 2
```

- Produce to a topic, with keys 
```  
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-events
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-events --property key.separator=: --property parse.key=true
```

- Consume from a topic, from beginning, printing offset and timestamp
```
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --property print.offset=true --property print.timestamp=true
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello-world --from-beginning
```

- Consumer group, mention group, list all groups and describe group
```
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --property print.offset=true --property print.timestamp=true --group name
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list 
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group demo-group-123 --describe
```

## Batching at Kafka Producer

- As and when we produce an event to topic, immediately the producer will not write to the kafka topic instead it would do it in batches and then producer would give it to kafka, it is the producer side behaviour.
- By default the batching is done every 1 second i.e. wait for 1 second, get all messages and then deliver all those messages.
```
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic hello-world --timeout 50
```
- The batching criteria could be dictated by either time i.e linger.ms=1000 or batch.size(bytes)>=16KB at the producer side, this is for kafka-console-producer.

Note:
- Consumer has to pull messages from Kafka topic.
- Pub-Sub pattern, Sub has to poll or pull messages from Kafka topic.
- KPL will serialise message to byte array before sending to Kafka topic and KCL will deserialise the byte array back to the original form when receiving message from Kafka.
    - Byte arrays mostly contain binary data.
    
## Offset

- Offset is specific to Kafka topic, there is a concept of Partition which is closely related with this.
- Kafka topic is an append only immutable structure i.e. as and when messages are sent to Kafka, Kafka will store the messages in the order it receives, the offset is a number which starts as 0 and messages are given the offset based on the order in which Kafka receives.
- When consumer asks for messages, Kafka will provide info based on offset.

- To print offset info as well for the message:
```
kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property print.offset=true
```

- Print message received by Kafka along with offset and timestamp:
```
kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property print.offset=true \
--property print.timestamp=true
```

- Print message received by Kafka along with offset and timestamp from beginning:
```
kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property print.offset=true \
--property print.timestamp=true
--from-beginning
```

## Consumer Group

- Consumer group: Logical collection of one or more consumers which will work together as a single consumer. Only one of the consumer among the consumer group would get the message.

```
kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property print.offset=true \
--group consumer_group_name
```

- If the consumer_group_name is same then only one consumer among the group of consumers would get the message and not every consumer in that group.

- Observation: One of the consumers in the consumer group was getting all traffic and other one was not, this can be solved using partition.

To view consumer groups:
```
kafka-consumer-groups.sh \
--bootstrap-server localhost:9092 \
--list
```

## Partition and Key

- When we create a topic, we have to mention how many partitions we want, if you don't mention, it is assumed to be 1 partition.
- Partition would help us with both message ordering and scaling at the same time.
- Along with partition, there is also a concept of key. Any message can have a key. key can be null or anything.
- All messages for a key will land to the same partition. This ensures ordering. There is no way a debit and credit event would land on different partitions for the same account (incase key is account_id)
- Incase of multiple consumers in a consumer group, Kafka would distribute partitions among them. This ensures scaling.
- Key should be chosen aptly for example, a key on user_id or account_id is better than key on date as that would otherwise send all events for a date to the same partition.
- The Apache Kafka client library determines which partition the message has to goto on basis of key value in the message.
```
partitionForKey = key % numPartitions
```

- Create a topic with 2 partitions:
```
kafka-topics.sh --bootstrap-server localhost:9092 --topic hello-world --create --partitions 2
```

- Describe topic to see partitions:
```
kafka-topics.sh --bootstrap-server localhost:9092 --topic hello-world --describe
```

Note:
- It is not that ONE broker would be leader for ALL partitions of a Kafka topic, there can be different leaders for partitions of a single topic i.e. one broker/kafka server could be leader for one partition of a topic and other broker could be leader of other partition of the same topic.

- Produce event with key:
```
kafka-console-producer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property key.separator=: \
--property parse.key=true
```

- Print key when consuming events:
```
kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property print.offset=true \
--property print.key=true \
--group name
```

- To consume events from the beginning for a consumer in a consumer group:
```
kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic hello-world \
--property print.offset=true \
--property print.key=true \
--group name \
--from-beginning
```

## Important Note

- Each partition maintains its own offset.
- Once a consumer leaves or joins a consumer group, Kafka does partition reassignment.

## Consumer Group Scaling

- The maximum number of consumers in a group can be equal to the number of partitions, this is because a partition cannot be assigned to more than one consumer in the group as that can cause ordering issue.
- We can alter the number of partitions for a topic however this comes with its own challenge as some key whose messages were going to a partition would now goto some other partition so it could happen some new message in the new partition is consumed before an old message in the old partition. There are few strategies such as moving to a new topic or stopping the producer to drain the existing message and then increase partition count.
- Reason to increase (alter) number of partitions could be because of increase in traffic.

## Consumer Group - LAG

- Kafka maintains status of how many messages have been produced (using offset) in each partition of a topic and how many messages of them have been consumed by the consumer group, and computes LAG accordingly. Kafka tracks each and every consumer group separately.
- To view status of consumer group and messages it has consumed and the lag:

```
kafka-consumer-groups.sh \
--bootstrap-server localhost:9092 \
--group name \
--describe
```

## Resetting Offset

- Since Kafka knows every consumer group and tracks what it has delivered so far, therefore
- --from-beginning flag won't work incase there is no lag.
- But sometime you would want to consume old messages again or messages from beginning (offset=0 for all partitions?).
- To consume messages from a particular offset:
- shift-by -5: Moves the offset by given N (for every partition of topic)
- by-duration PT5M: Moves the offset by 5 mins
- to-datetime 2023-01-01T00:00:00: moves the offset by datetime
- to-earliest: from beginning
- to-latest: latest

Dry run (to just see reset offset impact):
```
kafka-consumer-groups.sh \
--bootstrap-server localhost:9092 \
--group cg \
--topic hello-world \
--reset-offsets \
--shift-by -3 \
--dry-run
```

To execute reset offset:
```
kafka-consumer-groups.sh \
--bootstrap-server localhost:9092 \
--group cg \
--topic hello-world \
--reset-offsets \
--shift-by -3 \
--execute
```

## Message Format

- Message has these attributes: Key (can be nullable and can goto any partition), Value, Timestamp (if we don't set, Kafka producer would set), Partition & Offset, Headers (To provide additional metadata like Key/Value pairs) and Compression Type (none/gzip/..).

## Consumer Config

- Kafka stores everything in binary format (byte array), at consumer end we have to specify how to deserialise the key and value of the message.
- https://kafka.apache.org/documentation/#consumerconfigs

Note: By setting ConsumerConfig.AUTO_OFFSET_RESET_CONFIG as "earliest", we can consume events from beginning of a topic.

## Session Timeout Config

- Request joining group due to: need to re-join with the given member-id. 
- We get this message when Kafka consumer is restarted, the session timeout set for consumer is 45 seconds, i.e. if Kafka broker doesn't get a heartbeat from consumer every 45 seconds (by default) then only it would allow repartitioning of partitions to new or other consumers.
- https://kafka.apache.org/documentation/#connectconfigs_session.timeout.ms

## Acknowledging Message

- We have to ACK the message so as to update the current offset for the consumer group for a topic.
- Consumer collects all the ACKs and delivers same to Kafka broker every few seconds/periodically, this is controlled by auto.commit.interval.ms property of Kafka client. This commit is done every few seconds and not after every ACK because doing a network call to Kafka broker after every ACK for commit is not optimal.

## Out of Order Commit

- What if I skip ACK?
- If some messages before an offset are not acknowledged but the message after that (greater offset) is acknowledged then on lets say server restart or addition of new consumers in consumer group or whatever scenario those unacknowledged messages won't be redelivered.

Note:
- Producer has to serialise whereas consumer has to deserialise.

## Sender close

- You can close the sender/producer using .close once it has emitted all events, this is specific to application and not to be done in general.
- This closing of sender/producer can be done in .doOnComplete() method of sender reactive pipeline, this would stop the producer.

## Max In Flight

- maxInFlight is an alias for adjusting prefetch.
- In a reactive pipeline, things will be working based on the subscriber's back-pressure/processing speed i.e. if subscriber is slow then producer will not emit items at that pace.
- The back pressure is controlled by limitRate, by default this is 256 i.e. flux would emit 256 elements and it would wait for sender (Kafka producer in this case) to send these many to Kafka and then only more elements would be produced.

## Partition Assignment Strategy

- Cooperative Sticky Assignor: In CooperativeStickyAssignor partition assigning strategy, the partition would be assigned to the new consumer, from the consumer group, which was consuming from the most number of partitions, other consumers would be untouched in this strategy, this wasn't the case in the default strategy.
- The default partition strategy is RangeAssignor.
- In this strategy, incase any new consumer is added to the group, all partitions are sorted like [0, 1, 2] and the instance ids like [11, 12, 30] are sorted as well (consider any random numbers and 3 consumers), it would then assign these partitions to instances in range like 0th partition to 11-id consumer and 1st partition to 12-id consumer and 2nd partition to 30-id consumer.
- Therefore partitions assigned to other consumers could be changed on addition of new consumers i.e. other consumers are not untouched.

## More Kafka Notes

- Setup Kafka cluster using KRaft mode and see how Kafka cluster is H.A and horizontally scalable which would help to learn about replication and how Kafka is able to achieve availability.
- When we start a cluster, we specify role like this server is going to run as controller or broker or both broker + controller, so we can specify role for each and every node.
- Topic is a collection of partitions, so if we want perf and scaling, we would need to create topic with more partitions.
- When we want to create topic with 2 partitions, controller would identify 2 nodes as leaders for partition 0 and partition 1. We can specify replication.factor option for partition to be replicated in another broker node, this can be mentioned when creating a topic.
- cluster_metadata topic is an internal topic which maintains info about who is the leader and follower for every topic, each and every broker will be subscribing to this topic.

### Quick Note on Listeners

- Data plane: Broker talking among themselves.
- Control plane: Controller talking among themselves.
- These^ communication happen within the cluster.
- Kafka server can listen on multiple ports for multiple reasons such as one port maybe for control plane/data plane communication i.e. internal communication, maybe another port for just producing and consuming data i.e. external communication.

### Cluster Properties

- controller.quorum.voters
- The controllers would be choosing main controller among them, the above property in server.properties will contain the list of all controllers which are eligible for voting.
- Format=node-id@host-name:port-number-for-controller-communcation
- Example: controller.quorum.voters=1@kafka:9093

### Security Protocols

Kafka has 4 different security protocols:
- PLAINTEXT: All communication is going to be plaintext, no encryption.
- SSL: Kafka will encrypt communication.
- SASL_PLAINTEXT: Auth first (using username and password) then allow connection with no encryption.
- SASL_SSL: Auth first then allow connection with encryption.

### ISR

- ISR: In-sync replicas, i.e. whatever data is in leader (for a partition) is also in these replicas.

### Fault tolerant

- Using replication.factor, Kafka is more fault-tolerant.

### Producer ACKs

- Kafka broker sends an ACK when it ingests messages from the Kafka producer, this is internally managed by the KPL.
- ACK ensures that the message was successfully written in Leader and replicated to in-sync followers/replicas. This is the default behaviour.

### min.insync.replicas

- Min in sync replicas: Minimum number of nodes/replicas (including leader) which needs to successfully write the message to them so as to send an ACK to the producer.

### Idempotent Producer

- The Kafka producer library generates some sequence ids for the message when it tries to deliver it to the broker, incase the message is successfully written to broker but the ACK is not received by the producer maybe because of network issue then the producer would try to re-deliver the same message but this would NOT lead to duplicate entry since the same sequence ids would be sent to the broker for the message.

### Producing Duplicate Messages

- The enable.idempotence=true property of kafka library is from retry perspective i.e. this ensures no duplication of message at Kafka end and this is because of the same sequence ids generated for the message even on retry.
- But incase we are generating a NEW message which is duplicate of some previous message then in that case the sequence ids generated by the library would be different for that message and NOT same as earlier one, therefore duplicate message would be sent to Kafka broker, we have to be careful of that if app generated duplicate messages, it is not library's or Kafka broker's problem.

### Idempotent Consumer

- Incase the ACK from consumer doesn't reach the kafka broker for a particular message then the broker would assume that consumer has never received that message and would hence retry the message.
- It could happen that it was a credit card transaction event and the consumer would end up debiting amount from consumer twice incase of no idempotency at consumer side.

#### How to fix this?

- Let the producer set some unique message/event id (UUID) in the message payload or the message headers.
- Broker receives the message, they check corresponding to these ids if they are present in their DB.
- If yes, thats a duplicate, simply ack and skip processing.
- If no, its a new message, process and insert into db and then ack.
- This is how we can ensure that the consuming app is idempotent.

#### What if the message produced doesn't have a message id?

- The message produced has info like partition, offset, timestamp, etc, this info can be leveraged to create an id.

#### After how much time are the unacknowledged kafka messages redelivered to the same existing customer? OR After how much time would the broker retry if it doesn't receive ACK for a message from a consumer (in a consumer group)?

- If some messages before an offset are not acknowledged but the message after that (greater offset) is acknowledged then those unacknowledged messages won't be redelivered.
- As a consequence Kafka will not track the "un-acknowledged" messages in between.
- https://stackoverflow.com/questions/63705895/when-will-kafka-retry-to-process-the-messages-that-have-not-been-acknowledged

### Compression

- Compression is not enabled by default at producer side. compression.type=none by default i.e. compression is not enabled by default.
- Should we be always enabling compression? I would say we should test it once because compression is a time consuming or CPU intensive task.
- Consumer would also have to decompress the message. The consumer library automatically decompress when it sees a compressed message.

### How many topics should I create?

- If message ordering is necessary then we can choose single topic rather than having/dividing into multiple topics.
- For example: If we have topics specific to order operations like create, update, cancel, etc, there's a possibility that the update event might be processed before order creation event, therefore for the cases, where the operations are done on the same entity and the sequence of operations to be performed is crucial then we can go with single topic.

### How many partitions should I create?

- Based on consumer throughput
    - If a single consumer throughput is less than the events being produced then we can have multiple consumers to catch up with the events, since only 1 partition can be assigned to 1 consumers, we need to have lets say 'x' partitions for total events to be distributed among these 'x' partitions.
- Based on producer throughput
    - If the single consumer is capable of keeping up events being produced.
    - We know 1 partition will have 1 leader which is a broker, what about this producer-broker communication? Producer could be sending a lot of events to that broker and what if lets say that communication is slow, in this case, we can probably have more partitions i.e. more leaders/brokers and we would be able to reach the throughput for event being produced.

### Choosing replication factor

- Partition is for scaling and replica is for availability. The number of replicas to keep depends on how many brokers can go down at a time, if there can be maximum 5 such brokers (in a 100 node cluster) which can go down at a time then we can choose replication factor as 6.

## Batch/Parallel Processing

- receiveAutoAck: Once the queue of reactive type (flux) is drained then we would be automatically acknowledging to the Kafka broker.
- The commit interval for the ACKs to the broker from consumer is 5s by default.

### Parallel - flatMap

- Concat subscribes to Flux one by one.
- Flatmap subscribes to all Flux eagerly / at the same time so whatever the items are emitted, it is going to subscribe at the same time.
- By default flatmap can subscribe to 256 Flux(s)/publishers at the same time, can be changed via configuration.

### Message ordering issue with FlatMap

- If we are doing parallel processing using flatMap for the events being consumed and want to achieve ordering as well then we can use groupBy based on something.

## Error Handling

### Simple Retry

- We are doing a lot of disconnect and re-connect with Kafka incase of any error (like due to processing issue), we should be handling this in a separate pipeline.

### Separating Kafka Receiver and Processor

- To avoid above issue of frequent disconnect and reconnect on errors in kafka receiver pipeline, 
  - We can think like this - KafkaReceiver would give us a flux of items, when we get an item, we would be moving it into a separate processing pipeline, incase of error, the cancel signal would not be propagated to the main/receiver reactive pipeline, we can perform retry in this processing pipeline itself, BUT we would NOT be emitting error back to the receiver pipeline.

### Dead letter topic

- In onError() we can produce event to dead-letter-topic, this onError() function would be part of the processing reactive pipeline.
- A Kafka consumer can consume from multiple topics such as a topic and its DLT.

### Poison Pill Message

- Poison pill message: These messages are the ones which the consumer does not expect to consume from some topic, these messages can stop the consumer from further consuming other messages which it expects since these incorrect messages, which it is unable to process, are not acknowledged and consumer would keep on getting those on restarts.
- To counter above, we can write and use a custom deserialiser which doesn't fail on these incorrect type message and just logs them and returns a fallback value.

## Reactive Kafka with Spring

- Since KafkaReceiver and KafkaSender have a simple reactive pipeline hence no annotation available for same using Spring Reactive Kafka.

### Decoding

- When event is produced, a header called TypeId is appended with value equal to fully qualified name of package, this TypeId is required to deserialise the event.
- Kafka broker stores event as byte array.
- The consumer would be using the TypeId info to deserialise the object.

- Sometimes we may want to deserialise into custom type/pojo rather than what TypeId says, this can be configured in the ReceiverOptions.
```
// This kafkaProperties object is same what is mentioned in application.yaml
public ReceiverOptions<String, OrderEvent> receiverOptions(KafkaProperties kafkaProperties) {
return ReceiverOptions.<String, OrderEvent>create(kafkaProperties.buildConsumerProperties())
//.consumerProperty(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, "false") // by default, type id is removed after deserialization
.consumerProperty(JsonDeserializer.USE_TYPE_INFO_HEADERS, false)
.consumerProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, "someCustomClass")
.subscription(List.of("order-events"));
}
```

### Integration Testing

- For integration testing, we would be using EmbeddedKafka with Spring for Kafka server. Testcontainers could also be used.

To test producer application:
- We would mock the consumer application.
- We would use embedded kafka server and check if events are received by mocked consumer.

To test consumer application:
- We would mock the producer application.
- We would use embedded kafka server and check if events are received by consumer.

## How SSL works?

- We have a CA (Certificate Authority) like GoDaddy, and has its private and public keys.
- Our website/server, using a private key, would be sending request to CA to get the SSL certificate.
- The clients or users would be sending request to our website like https://mybank.com, our website would be responding back with one SSL cert, the client/user browser would validate the SSL cert sent by our website with CA.
- Our website/server would also be sending public key along with the SSL cert, using this key the client can encrypt the request and no one can decrypt the request in between, in order to decrypt, we have to have the private key and only the server has that private key.
