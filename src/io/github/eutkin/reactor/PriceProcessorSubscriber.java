package io.github.eutkin.reactor;

import io.github.eutkin.PriceProcessor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;

/**
 * Simple {@link  java.util.concurrent.Flow.Subscriber} adapter for {@link PriceProcessor}.
 */
public class PriceProcessorSubscriber implements Flow.Subscriber<Map.Entry<String, Double>> {

    private final PriceProcessor priceProcessor;

    private Flow.Subscription subscription;

    public PriceProcessorSubscriber(PriceProcessor priceProcessor) {
        this.priceProcessor = priceProcessor;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(Map.Entry<String, Double> item) {
        this.priceProcessor.onPrice(item.getKey(), item.getValue());
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceProcessorSubscriber that = (PriceProcessorSubscriber) o;
        return Objects.equals(priceProcessor, that.priceProcessor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priceProcessor);
    }

}
