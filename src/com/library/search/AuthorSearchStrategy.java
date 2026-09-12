package com.library.search;

import com.library.domain.model.LibraryItem;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matches on the contributor: the author of a book, the publisher of a magazine.
 * It reads {@code getContributor()} rather than casting to {@code Book}, so the
 * strategy works for any format the catalogue holds.
 */
public class AuthorSearchStrategy implements SearchStrategy {

    @Override
    public List<LibraryItem> search(Collection<LibraryItem> items, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        if (needle.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item.getContributor() != null
                        && item.getContributor().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "Author";
    }
}
