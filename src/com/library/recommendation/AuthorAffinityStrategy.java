package com.library.recommendation;

import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Suggests other titles by authors the patron has already borrowed.
 *
 * <p>Collects the contributors from the history into a {@link HashSet}, giving
 * O(1) membership tests while the catalogue is scanned once.</p>
 */
public class AuthorAffinityStrategy implements RecommendationStrategy {

    @Override
    public List<LibraryItem> recommend(Patron patron, List<LibraryItem> catalogue, int limit) {
        Map<String, LibraryItem> byId = new HashMap<>();
        for (LibraryItem item : catalogue) {
            byId.put(item.getId(), item);
        }

        Set<String> favouredContributors = new HashSet<>();
        for (String borrowedId : patron.getDistinctBorrowedItems()) {
            LibraryItem item = byId.get(borrowedId);
            if (item != null && item.getContributor() != null) {
                favouredContributors.add(item.getContributor().toLowerCase());
            }
        }
        if (favouredContributors.isEmpty()) {
            return List.of();
        }

        Set<String> alreadyRead = patron.getDistinctBorrowedItems();
        return catalogue.stream()
                .filter(item -> !alreadyRead.contains(item.getId()))
                .filter(item -> item.getContributor() != null
                        && favouredContributors.contains(item.getContributor().toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "Same author";
    }
}
