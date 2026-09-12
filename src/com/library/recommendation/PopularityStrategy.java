package com.library.recommendation;

import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;
import com.library.repository.LoanRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Suggests the most borrowed titles the patron has not read.
 *
 * <p>Serves as the cold-start fallback: it needs nothing from the patron's own
 * history, so a member who has borrowed nothing still receives suggestions.
 * Loan counts are tallied into a {@link HashMap} in a single O(n) pass over the
 * loan ledger.</p>
 */
public class PopularityStrategy implements RecommendationStrategy {

    private final LoanRepository loanRepository;

    public PopularityStrategy(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public List<LibraryItem> recommend(Patron patron, List<LibraryItem> catalogue, int limit) {
        Map<String, Integer> borrowCounts = new HashMap<>();
        loanRepository.findAll()
                .forEach(loan -> borrowCounts.merge(loan.getItemId(), 1, Integer::sum));

        Set<String> alreadyRead = patron.getDistinctBorrowedItems();
        return catalogue.stream()
                .filter(item -> !alreadyRead.contains(item.getId()))
                .filter(item -> borrowCounts.containsKey(item.getId()))
                .sorted(Comparator
                        .comparingInt((LibraryItem item) -> borrowCounts.getOrDefault(item.getId(), 0))
                        .reversed()
                        .thenComparing(LibraryItem::getTitle))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "Most borrowed";
    }
}
