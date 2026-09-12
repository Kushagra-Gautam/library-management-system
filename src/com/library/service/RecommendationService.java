package com.library.service;

import com.library.domain.model.LibraryItem;
import com.library.recommendation.RecommendationStrategy;

import java.util.List;

/**
 * Suggests titles to a patron.
 */
public interface RecommendationService {

    /**
     * @param patronId who the suggestions are for
     * @param limit    the most suggestions wanted
     * @return suggestions from the default blended strategy
     */
    List<LibraryItem> recommendFor(String patronId, int limit);

    /**
     * @param patronId who the suggestions are for
     * @param strategy the strategy to use instead of the default
     * @param limit    the most suggestions wanted
     * @return suggestions from that strategy
     */
    List<LibraryItem> recommendFor(String patronId, RecommendationStrategy strategy, int limit);

    /**
     * @return the strategies available to choose from
     */
    List<RecommendationStrategy> getAvailableStrategies();
}
