package com.library.recommendation;

import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Blends several strategies into one ranked list.
 *
 * <p>Each delegate contributes points to the titles it suggests: a title placed
 * first by a strategy earns more than one placed last, and the whole
 * contribution is multiplied by that strategy's weight. Titles suggested by more
 * than one strategy therefore rise to the top, which is the behaviour a reader
 * expects.</p>
 *
 * <p>Being a {@link RecommendationStrategy} itself, it can be used anywhere a
 * single strategy can, including inside another composite.</p>
 */
public class CompositeRecommendationStrategy implements RecommendationStrategy {

    private final Map<RecommendationStrategy, Double> weightedStrategies = new LinkedHashMap<>();

    /**
     * @param strategy a delegate to include
     * @param weight   how much its opinion counts
     * @return this composite, for chaining
     */
    public CompositeRecommendationStrategy add(RecommendationStrategy strategy, double weight) {
        if (strategy != null && weight > 0) {
            weightedStrategies.put(strategy, weight);
        }
        return this;
    }

    @Override
    public List<LibraryItem> recommend(Patron patron, List<LibraryItem> catalogue, int limit) {
        Map<LibraryItem, Double> scores = new LinkedHashMap<>();
        int poolSize = Math.max(limit * 2, 10);

        for (Map.Entry<RecommendationStrategy, Double> entry : weightedStrategies.entrySet()) {
            List<LibraryItem> suggestions = entry.getKey().recommend(patron, catalogue, poolSize);
            double weight = entry.getValue();
            for (int rank = 0; rank < suggestions.size(); rank++) {
                double positionScore = (double) (suggestions.size() - rank) / suggestions.size();
                scores.merge(suggestions.get(rank), positionScore * weight, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<LibraryItem, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getTitle()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * @return the delegates in registration order
     */
    public List<RecommendationStrategy> getStrategies() {
        return new ArrayList<>(weightedStrategies.keySet());
    }

    @Override
    public String getStrategyName() {
        return "Blended (" + weightedStrategies.size() + " signals)";
    }
}
