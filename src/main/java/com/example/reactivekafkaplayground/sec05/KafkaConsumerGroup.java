package com.example.reactivekafkaplayground.sec05;

/*
 * Create a topic with 3 partitions
 * Max number of consumers in consumer group = number of partitions
 * */
public class KafkaConsumerGroup {

	// kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --partitions 3 --create
	// kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --describe

	private static class Consumer1 {
		public static void main(String[] args) {
			KafkaConsumer.startConsumer("1");
		}
	}

	private static class Consumer2 {
		public static void main(String[] args) {
			KafkaConsumer.startConsumer("2");
		}
	}

	private static class Consumer3 {
		public static void main(String[] args) {
			KafkaConsumer.startConsumer("3");
		}
	}
}
