package com.library.search;

import com.library.domain.model.LibraryItem;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matches on the catalogue key, ignoring hyphens and spacing so that
 * {@code 978-0132350884} and {@code 9780132350884} both find the same record.
 */
public class IsbnSearchStrategy implements SearchStrategy {

    @Override
    public List<LibraryItem> search(Collection<LibraryItem> items, String query) {
        String needle = normalise(query);
        if (needle.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> normalise(item.getId()).contains(needle))
                .collect(Collectors.toList());
    }

    private String normalise(String value) {
        return value == null ? "" : value.replace("-", "").replace(" ", "").toLowerCase();
    }

    @Override
    public String getName() {
        return "ISBN";
    }
}
