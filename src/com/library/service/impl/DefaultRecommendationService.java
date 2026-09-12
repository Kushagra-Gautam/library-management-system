package com.library.service.impl;

import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;
import com.library.recommendation.RecommendationStrategy;
import com.library.repository.ItemRepository;
import com.library.service.PatronService;
import com.library.service.RecommendationService;
import com.library.util.LoggingConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Default recommendation implementation.
 *
 * <p>Holds a default strategy and a list of alternatives, all behind the
 * {@link RecommendationStrategy} interface. Adding a new signal means
 * registering one more strategy at wiring time; nothing here changes.</p>
 */
public class DefaultRecommendationService implements RecommendationService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultRecommendationService.class);

    private final ItemRepository itemRepository;
    private final PatronService patronService;
    private final RecommendationStrategy defaultStrategy;
    private final List<RecommendationStrategy> availableStrategies = new ArrayList<>();

    public DefaultRecommendationService(ItemRepository itemRepository, PatronService patronService,
                                        RecommendationStrategy defaultStrategy) {
        this.itemRepository = itemRepository;
        this.patronService = patronService;
        this.defaultStrategy = defaultStrategy;
        this.availableStrategies.add(defaultStrategy);
    }

    /**
     * @param strategy an alternative the operator can pick from the menu
     */
    public void registerStrategy(RecommendationStrategy strategy) {
        if (strategy != null && !availableStrategies.contains(strategy)) {
            availableStrategies.add(strategy);
        }
    }

    @Override
    public List<LibraryItem> recommendFor(String patronId, int limit) {
        return recommendFor(patronId, defaultStrategy, limit);
    }

    @Override
    public List<LibraryItem> recommendFor(String patronId, RecommendationStrategy strategy, int limit) {
        Patron patron = patronService.findById(patronId);
        List<LibraryItem> catalogue = itemRepository.findAll();
        List<LibraryItem> suggestions = strategy.recommend(patron, catalogue, Math.max(1, limit));
        LOGGER.fine(() -> String.format("[%s] produced %d suggestion(s) for %s",
                strategy.getStrategyName(), suggestions.size(), patronId));
        return suggestions;
    }

    @Override
    public List<RecommendationStrategy> getAvailableStrategies() {
        return new ArrayList<>(availableStrategies);
    }
}
