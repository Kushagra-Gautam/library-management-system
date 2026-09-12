package com.library.service.impl;

import com.library.domain.enums.EventType;
import com.library.domain.enums.Genre;
import com.library.domain.model.Book;
import com.library.domain.model.LibraryItem;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.DuplicateItemException;
import com.library.exception.ItemNotFoundException;
import com.library.factory.LibraryItemFactory;
import com.library.repository.ItemRepository;
import com.library.search.SearchStrategy;
import com.library.service.CatalogService;
import com.library.util.LoggingConfig;
import com.library.util.Validator;

import java.util.List;
import java.util.logging.Logger;

/**
 * Default catalogue implementation.
 *
 * <p>Depends on the {@link ItemRepository} abstraction and on whichever
 * {@link SearchStrategy} it is handed, so it knows neither how records are
 * stored nor how matching is decided.</p>
 */
public class DefaultCatalogService implements CatalogService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultCatalogService.class);

    private final ItemRepository itemRepository;
    private final EventPublisher eventPublisher;

    public DefaultCatalogService(ItemRepository itemRepository, EventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LibraryItem addItem(LibraryItem item) {
        if (itemRepository.existsById(item.getId())) {
            LOGGER.warning(() -> "Rejected duplicate catalogue key " + item.getId());
            throw new DuplicateItemException(item.getId());
        }
        itemRepository.save(item);
        LOGGER.info(() -> "Catalogued " + item.getItemType() + " " + item.getId()
                + " - " + item.getTitle());
        eventPublisher.publish(new LibraryEvent(EventType.ITEM_CATALOGUED, item.getId(), null,
                "Catalogued '" + item.getTitle() + "'"));
        return item;
    }

    @Override
    public Book addBook(String isbn, String title, String author, int year, Genre genre) {
        Book book = LibraryItemFactory.createBook(isbn, title, author, year, genre);
        addItem(book);
        return book;
    }

    @Override
    public LibraryItem updateItem(String itemId, String title, int year, Genre genre) {
        LibraryItem item = findById(itemId);
        if (!Validator.isBlank(title)) {
            item.setTitle(Validator.requireName(title, "title"));
        }
        if (year > 0) {
            item.setPublicationYear(Validator.requireYear(year));
        }
        if (genre != null) {
            item.setGenre(genre);
        }
        itemRepository.save(item);
        LOGGER.info(() -> "Updated catalogue record " + itemId);
        return item;
    }

    @Override
    public boolean removeItem(String itemId) {
        findById(itemId);
        boolean removed = itemRepository.deleteById(itemId);
        LOGGER.info(() -> "Withdrew catalogue record " + itemId);
        return removed;
    }

    @Override
    public LibraryItem findById(String itemId) {
        return itemRepository.findById(Validator.requireText(itemId, "item id"))
                .orElseThrow(() -> new ItemNotFoundException(itemId));
    }

    @Override
    public List<LibraryItem> search(SearchStrategy strategy, String query) {
        List<LibraryItem> results = strategy.search(itemRepository.findAll(), query);
        LOGGER.fine(() -> String.format("Search [%s] for '%s' returned %d result(s)",
                strategy.getName(), query, results.size()));
        return results;
    }

    @Override
    public List<LibraryItem> listAll() {
        return itemRepository.findAll();
    }

    @Override
    public int getCatalogueSize() {
        return itemRepository.count();
    }
}
