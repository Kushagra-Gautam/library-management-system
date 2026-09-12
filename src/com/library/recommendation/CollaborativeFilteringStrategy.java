package com.library.recommendation;

import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;
import com.library.repository.PatronRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Item-to-item collaborative filtering: "readers who borrowed this also borrowed".
 *
 * <p>For each other patron, the overlap with this patron's history is measured
 * by set intersection. Titles held by a neighbour but not by this patron are
 * then scored by the size of that overlap, so the more reading two members share
 * the more weight that neighbour's other titles carry.</p>
 *
 * <p>Both histories are held in {@link HashSet}s, making each membership test
 * O(1) and the whole pass O(p × h) for p patrons and h titles per history —
 * linear in the data actually present, with no pairwise matrix to build.</p>
 */
public class CollaborativeFilteringStrategy implements RecommendationStrategy {

    private static final int MIN_OVERLAP = 1;

    private final PatronRepository patronRepository;

    public CollaborativeFilteringStrategy(PatronRepository patronRepository) {
        this.patronRepository = patronRepository;
    }

    @Override
    public List<LibraryItem> recommend(Patron patron, List<LibraryItem> catalogue, int limit) {
        Set<String> mine = new HashSet<>(patron.getDistinctBorrowedItems());
        if (mine.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> scores = new HashMap<>();
        for (Patron neighbour : patronRepository.findAll()) {
            if (neighbour.getPatronId().equals(patron.getPatronId())) {
                continue;
            }
            Set<String> theirs = new HashSet<>(neighbour.getDistinctBorrowedItems());
            int overlap = countOverlap(mine, theirs);
            if (overlap < MIN_OVERLAP) {
                continue;
            }
            for (String candidate : theirs) {
                if (!mine.contains(candidate)) {
                    scores.merge(candidate, overlap, Integer::sum);
                }
            }
        }
        if (scores.isEmpty()) {
            return List.of();
        }

        Map<String, LibraryItem> byId = catalogue.stream()
                .collect(Collectors.toMap(LibraryItem::getId, item -> item, (a, b) -> a));

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> byId.get(entry.getKey()))
                .filter(java.util.Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Iterates the smaller set so the cost is bounded by the shorter history.
     */
    private int countOverlap(Set<String> first, Set<String> second) {
        Set<String> smaller = first.size() <= second.size() ? first : second;
        Set<String> larger = smaller == first ? second : first;
        int overlap = 0;
        for (String value : smaller) {
            if (larger.contains(value)) {
                overlap++;
            }
        }
        return overlap;
    }

    @Override
    public String getStrategyName() {
        return "Readers also borrowed";
    }
}
