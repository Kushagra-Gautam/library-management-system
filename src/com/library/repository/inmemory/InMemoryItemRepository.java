package com.library.repository.inmemory;

import com.library.domain.model.LibraryItem;
import com.library.repository.ItemRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory catalogue.
 */
public class InMemoryItemRepository extends InMemoryRepository<LibraryItem, String>
        implements ItemRepository {

    public InMemoryItemRepository() {
        super(LibraryItem::getId);
    }

    @Override
    public <T extends LibraryItem> List<T> findByType(Class<T> type) {
        return findAll().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }
}
