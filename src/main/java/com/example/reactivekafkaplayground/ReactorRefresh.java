package com.example.reactivekafkaplayground;

import reactor.core.publisher.Flux;

public class ReactorRefresh {
	public static void main(String[] args) throws InterruptedException {
		/*Flux.just(1, 2, 3, 4, 5)
				.log()
				.take(2) // take only 2 elements
				.log()
				.subscribe(integer -> System.out.println("Re: " + integer));
*/
		/*Flux.interval(Duration.ofMillis(1000)) // every 1 second emit one element
				.log()
				.take(5)
				.log()
				.subscribe(integer -> System.out.println("Re: " + integer));
*/

		/*Flux.just(1, 2, 3, 4, 5, 6, 7, 8)
				//.log()
				.take(5) // take only 2 elements
				.log()
				.subscribe(new Subscriber<Integer>() {
					@Override
					public void onSubscribe(Subscription subscription) {
						// Bounding to only 5 elements
						// subscription.request(5);
						subscription.request(Long.MAX_VALUE); // Long.MAX_VALUE is unbounded
					}

					@Override
					public void onNext(Integer integer) {
						System.out.println("Re: " + integer);
					}

					@Override
					public void onError(Throwable throwable) {
						System.out.println(throwable.getMessage());
					}

					@Override
					public void onComplete() {
						System.out.println("Completed");
					}
				});*/

		/*Flux.range(1, 1000)
				.log()
				//.limitRate(250) // subscriber specific property to control fetching rate from producer
				.map(i -> i * 10)
				//.log()
				.subscribe();*/

		Flux.range(1, 5)
				.log()
				.map(i -> i / 0)
				.doOnError(e -> System.out.println("Hey! Error"))
				// anything failing above would be retried 2 times
				.retry(2)
				.log()
				.subscribe(integer -> System.out.println("Number processed: " + integer), e -> System.out.println("Hey! Error (Inside Subscriber)"));

		// request(unbounded)
		// onNext(1) --> the publisher emitted the event
		// cancel() --> the subscriber issues cancel event to the upstream or publisher because of some exception
		// error() --> error was propagate to consumer
		// thrice request(unbounded) was called by the subscriber

		//Thread.sleep(10000);
	}
}
