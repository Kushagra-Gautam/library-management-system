package com.library.recommendation;

import com.library.domain.enums.Genre;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Suggests titles in the genres a patron reads most.
 *
 * <p>Builds a genre histogram from the borrowing history in one pass, using an
 * {@link EnumMap} — an array under the hood, so counting is O(1) per borrowing
 * and the whole scoring pass is O(n) in history length. Stated genre preferences
 * are folded in with a fixed weight so a brand new member still gets sensible
 * suggestions.</p>
 */
public class GenreAffinityStrategy implements RecommendationStrategy {

    private static final int STATED_PREFERENCE_WEIGHT = 2;

    @Override
    public List<LibraryItem> recommend(Patron patron, List<LibraryItem> catalogue, int limit) {
        Map<String, LibraryItem> byId = catalogue.stream()
                .collect(Collectors.toMap(LibraryItem::getId, item -> item, (a, b) -> a));

        Map<Genre, Integer> affinity = new EnumMap<>(Genre.class);
        for (String borrowedId : patron.getBorrowingHistory()) {
            LibraryItem item = byId.get(borrowedId);
            if (item != null && item.getGenre() != null) {
                affinity.merge(item.getGenre(), 1, Integer::sum);
            }
        }
        for (Genre stated : patron.getPreferredGenres()) {
            affinity.merge(stated, STATED_PREFERENCE_WEIGHT, Integer::sum);
        }
        if (affinity.isEmpty()) {
            return List.of();
        }

        Set<String> alreadyRead = patron.getDistinctBorrowedItems();
        return catalogue.stream()
                .filter(item -> !alreadyRead.contains(item.getId()))
                .filter(item -> item.getGenre() != null && affinity.containsKey(item.getGenre()))
                .sorted(Comparator
                        .comparingInt((LibraryItem item) -> affinity.getOrDefault(item.getGenre(), 0))
                        .reversed()
                        .thenComparing(LibraryItem::getTitle))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "Genre affinity";
    }
}
