package io.github.eutkin.reactor;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class App {

    public static void main(String[] args) throws InterruptedException {
        final var publisher = new PriceThrottler();

        publisher.subscribe((ccyPair, rate) ->
                System.out.println(LocalTime.now() + " Fast: ccyPair : " + ccyPair + ", rate: " + rate));

        publisher.subscribe((ccyPair, rate) -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            System.out.println(LocalTime.now() + " Slow: ccyPair : " + ccyPair + ", rate: " + rate);
        });

        try (publisher) {
            for (int i = 0; i < 100; i++) {
                final ThreadLocalRandom random = ThreadLocalRandom.current();
                final Map.Entry<String, Double> entry = Map.entry("EUR/USD", random.nextDouble(10, 200));
                System.out.println("Current: " + entry);
                publisher.onPrice(entry.getKey(), entry.getValue());

                Thread.sleep(random.nextLong(1, 500));
            }

        }

        Thread.sleep(10000);
    }
}
