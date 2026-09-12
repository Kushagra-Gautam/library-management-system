package com.library.service;

import com.library.domain.model.Book;
import com.library.domain.model.LibraryItem;
import com.library.search.SearchStrategy;

import java.util.List;

/**
 * Manages catalogue records: adding, updating, removing and searching.
 */
public interface CatalogService {

    /**
     * @param item the record to catalogue
     * @return the catalogued record
     */
    LibraryItem addItem(LibraryItem item);

    /**
     * @param isbn   catalogue key
     * @param title  book title
     * @param author the author
     * @param year   year of publication
     * @param genre  subject classification
     * @return the catalogued book
     */
    Book addBook(String isbn, String title, String author, int year,
                 com.library.domain.enums.Genre genre);

    /**
     * @param itemId the record to change
     * @param title  new title, or {@code null} to keep the current one
     * @param year   new year, or zero to keep the current one
     * @param genre  new genre, or {@code null} to keep the current one
     * @return the updated record
     */
    LibraryItem updateItem(String itemId, String title, int year,
                           com.library.domain.enums.Genre genre);

    /**
     * @param itemId the record to withdraw
     * @return {@code true} when it was removed
     */
    boolean removeItem(String itemId);

    /**
     * @param itemId the catalogue key
     * @return the record
     */
    LibraryItem findById(String itemId);

    /**
     * @param strategy how to match
     * @param query    what the user typed
     * @return matching records
     */
    List<LibraryItem> search(SearchStrategy strategy, String query);

    /**
     * @return every catalogue record
     */
    List<LibraryItem> listAll();

    int getCatalogueSize();
}
