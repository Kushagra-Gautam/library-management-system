package com.library.factory;

import com.library.domain.enums.Genre;
import com.library.domain.model.Book;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Magazine;
import com.library.exception.InvalidInputException;
import com.library.util.Validator;

import java.util.Map;

/**
 * Factory pattern: builds catalogue records from loosely typed input.
 *
 * <p>Callers name the format they want rather than the class that implements it,
 * so the console, the demo runner and any future importer all create items the
 * same way. Validation happens here once, which means an invalid record cannot
 * reach the catalogue by a side door.</p>
 */
public final class LibraryItemFactory {

    private LibraryItemFactory() {
    }

    /**
     * @param format     "book" or "magazine"
     * @param attributes the field values, keyed by name
     * @return the constructed catalogue record
     * @throws InvalidInputException when the format is unknown or a field is invalid
     */
    public static LibraryItem create(String format, Map<String, String> attributes) {
        String kind = Validator.requireText(format, "item format").toLowerCase();
        switch (kind) {
            case "book":
                return createBook(attributes);
            case "magazine":
                return createMagazine(attributes);
            default:
                throw new InvalidInputException("item format",
                        "'" + format + "' is not supported; expected book or magazine");
        }
    }

    private static LibraryItem createBook(Map<String, String> attributes) {
        String isbn = Validator.requireIsbn(attributes.get("isbn"));
        String title = Validator.requireName(attributes.get("title"), "title");
        String author = Validator.requireName(attributes.getOrDefault("author", "Unknown"), "author");
        int year = Validator.requireYear(parseYear(attributes.get("year")));
        Genre genre = Genre.fromText(attributes.get("genre"));

        return new Book.Builder(isbn, title)
                .author(author)
                .publicationYear(year)
                .genre(genre == null ? Genre.FICTION : genre)
                .publisher(attributes.getOrDefault("publisher", ""))
                .edition(attributes.getOrDefault("edition", "1st"))
                .pageCount(attributes.containsKey("pages")
                        ? Validator.parseInt(attributes.get("pages"), "pages") : 0)
                .build();
    }

    private static LibraryItem createMagazine(Map<String, String> attributes) {
        String issn = Validator.requireText(attributes.get("isbn"), "ISSN");
        String title = Validator.requireName(attributes.get("title"), "title");
        String publisher = Validator.requireName(
                attributes.getOrDefault("publisher", "Unknown"), "publisher");
        int year = Validator.requireYear(parseYear(attributes.get("year")));
        Genre genre = Genre.fromText(attributes.get("genre"));
        int issue = attributes.containsKey("issue")
                ? Validator.parseInt(attributes.get("issue"), "issue number") : 1;

        return new Magazine(issn, title, year, genre == null ? Genre.NON_FICTION : genre,
                publisher, issue);
    }

    private static int parseYear(String value) {
        if (Validator.isBlank(value)) {
            return java.time.Year.now().getValue();
        }
        return Validator.parseInt(value, "publication year");
    }

    /**
     * Convenience overload for the common case of cataloguing a book.
     *
     * @return the constructed book
     */
    public static Book createBook(String isbn, String title, String author, int year, Genre genre) {
        return new Book.Builder(Validator.requireIsbn(isbn), Validator.requireName(title, "title"))
                .author(Validator.requireName(author, "author"))
                .publicationYear(Validator.requireYear(year))
                .genre(genre)
                .build();
    }
}
