package io.github.eutkin.reactor;

import io.github.eutkin.PriceProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class PriceThrottler implements PriceProcessor, AutoCloseable {

    /**
     * Subscriptions are stored in ConcurrentHashMap for later unsubscription.
     */
    private final Map<PriceProcessor, PriceSubscription> subscriptions = new ConcurrentHashMap<>();

    /**
     * need a Thread Pool for subscribers to work independently.
     * CachedThreadPool was chosen as the implementation, since we don't know anything about the speed of subscribers.
     */
    /* FIXME: You should have two Thread Pools, one for fast subscribers and one for slow subscribers.
        Run each subscriber on Thread Pool for slow subscribers and execute it on Thread Pool for fast subscribers
        after collecting statistics about its execution time.
     */
    private final ExecutorService pool = Executors.newCachedThreadPool();


    /**
     * Updates ccy pair in each subscription.
     *
     * @param ccyPair - EURUSD, EURRUB, USDJPY - up to 200 different currency pairs
     * @param rate    - any double rate like 1.12, 200.23 etc
     */
    @Override
    public void onPrice(String ccyPair, double rate) {
        this.subscriptions.forEach((key, subscription) -> subscription.rates.put(ccyPair, rate));
    }


    /**
     * We use {@link ConcurrentHashMap#computeIfAbsent} to atomically create and subscribe the PriceProcessor.
     * If the subscription for this PriceProcessor already exists, we do nothing.
     *
     * @param priceProcessor - subscriber
     */
    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        this.subscriptions.computeIfAbsent(priceProcessor, key -> {
            final var subscriber = new PriceProcessorSubscriber(priceProcessor);
            final var subscription = new PriceSubscription(subscriber);
            subscriber.onSubscribe(subscription);
            return subscription;
        });
    }

    /**
     * Remove subscribers.
     *
     * @param priceProcessor subscriber for removing.
     */
    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        this.subscriptions.remove(priceProcessor);
    }

    @Override
    public void close() throws InterruptedException {
        if (!this.pool.awaitTermination(10, TimeUnit.SECONDS)) {
            this.pool.shutdownNow();
        }
    }


    private final class PriceSubscription implements Flow.Subscription {

        private final Flow.Subscriber<Map.Entry<String, Double>> subscriber;

        /**
         * Last rate for each ccy pair.
         */
        private final Map<String, Double> rates = new ConcurrentHashMap<>();

        private PriceSubscription(Flow.Subscriber<Map.Entry<String, Double>> subscriber) {
            this.subscriber = subscriber;
        }

        // FIXME: Ignore count of items – its no good.
        @Override
        public void request(long n) {
            // each subscriber work in separate thread
            if (PriceThrottler.this.pool.isShutdown()) {
                return;
            }
            PriceThrottler.this.pool.execute(() -> {
                // spin lock to wait for a pair to appear
                // FIXME: Spin Lock requires too many resources
                while (this.rates.isEmpty() && !Thread.currentThread().isInterrupted()) {
                    // FIXME: Need a time-out mechanism to prevent a leaks 
                }
                for (String ccy : this.rates.keySet()) {
                    // Remove the old rate, so as not to rework it
                    // FIXME:  Need synchronization between Map#entrySet and Map#remove
                    final Double rate = this.rates.remove(ccy);
                    if (rate != null)
                        // FIXME: If the implementation PriceProccesor#onPrice  is asynchronous, there will be problems
                        this.subscriber.onNext(Map.entry(ccy, rate));
                }
            });
        }

        @Override
        public void cancel() {
            // FIXME: Need mechanism for cancelling subscription
        }
    }
}
