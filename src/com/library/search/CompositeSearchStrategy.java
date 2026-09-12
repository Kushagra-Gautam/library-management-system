package com.library.search;

import com.library.domain.model.LibraryItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Composite: runs several strategies and unions their results.
 *
 * <p>It implements the very interface it aggregates, so a caller cannot tell a
 * composite from a single strategy — which is what lets the catalogue offer a
 * "search everything" option without any special case in the service.</p>
 */
public class CompositeSearchStrategy implements SearchStrategy {

    private final List<SearchStrategy> strategies = new ArrayList<>();

    public CompositeSearchStrategy(SearchStrategy... strategies) {
        for (SearchStrategy strategy : strategies) {
            if (strategy != null) {
                this.strategies.add(strategy);
            }
        }
    }

    /**
     * @param strategy another strategy to include in the union
     * @return this composite, for chaining
     */
    public CompositeSearchStrategy add(SearchStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
        }
        return this;
    }

    @Override
    public List<LibraryItem> search(Collection<LibraryItem> items, String query) {
        Set<LibraryItem> merged = new LinkedHashSet<>();
        for (SearchStrategy strategy : strategies) {
            merged.addAll(strategy.search(items, query));
        }
        return new ArrayList<>(merged);
    }

    @Override
    public String getName() {
        return "Any field";
    }
}
