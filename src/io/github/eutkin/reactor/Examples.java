package io.github.eutkin.reactor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Examples {

    public static void main(String[] args) throws InterruptedException {
        final var priceThrottler = new PriceThrottler();

        priceThrottler.subscribe(
                (ccyPair, rate) -> System.out.println("Fast: ccyPair : " + ccyPair + ", rate: " + rate)
        );

        priceThrottler.subscribe((ccyPair, rate) -> {
            System.out.println("Slow: ccyPair : " + ccyPair + ", rate: " + rate);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            System.out.println("Completed the slow: ccyPair : " + ccyPair + ", rate: " + rate);
        });


        final var ccyPairs = List.of("EUR/USD", "EUR/RUB");
        try(priceThrottler) {
            for (int i = 0; i < 10; i++) {
                final ThreadLocalRandom random = ThreadLocalRandom.current();
                final String randomCcyPair = ccyPairs.get(random.nextInt(0, ccyPairs.size()));
                final double randomRate = random.nextDouble(1, 2);
                final Map.Entry<String, Double> entry = Map.entry(randomCcyPair, randomRate);
                System.out.println("Current: " + entry);
                priceThrottler.onPrice(entry.getKey(), entry.getValue());

                Thread.sleep(random.nextLong(1, 500));
            }
        }
    }
}
