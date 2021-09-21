package io.github.eutkin.reactor;

import io.github.eutkin.PriceProcessor;

import java.util.Map;
import java.util.concurrent.SubmissionPublisher;

public class PriceThrottler implements PriceProcessor, AutoCloseable {

   private final SubmissionPublisher<Map.Entry<String, Double>> sink = new SubmissionPublisher<>();

    @Override
    public void onPrice(String ccyPair, double rate) {
        this.sink.submit(Map.entry(ccyPair, rate));
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        this.sink.subscribe(new PriceProcessorSubscriber(priceProcessor));
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        this.sink.getSubscribers().remove(new PriceProcessorSubscriber(priceProcessor));
    }

    @Override
    public void close() {
        this.sink.close();
    }
}
