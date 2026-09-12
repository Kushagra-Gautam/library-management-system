package com.library.recommendation;

import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;

import java.util.List;

/**
 * Strategy pattern: one way of suggesting titles to a patron.
 *
 * <p>Each implementation gets the whole catalogue and the patron, and returns a
 * ranked list. Because they share this shape, they can be weighed against one
 * another and blended by {@link CompositeRecommendationStrategy} without any of
 * them knowing that the others exist.</p>
 */
public interface RecommendationStrategy {

    /**
     * @param patron    who the suggestions are for
     * @param catalogue every catalogue record
     * @param limit     the most suggestions wanted
     * @return suggested items, best first
     */
    List<LibraryItem> recommend(Patron patron, List<LibraryItem> catalogue, int limit);

    /**
     * @return a label for menus and explanations
     */
    String getStrategyName();
}
