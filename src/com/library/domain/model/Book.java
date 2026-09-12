package com.library.domain.model;

import com.library.domain.enums.Genre;

/**
 * A catalogued book, identified by its ISBN.
 *
 * <p>Instances are created through {@link Builder} rather than a long
 * constructor, so a caller cannot silently transpose two same-typed arguments
 * and every optional field has a sensible default.</p>
 */
public class Book extends LibraryItem {

    private String author;
    private String publisher;
    private String edition;
    private int pageCount;

    private Book(Builder builder) {
        super(builder.isbn, builder.title, builder.publicationYear, builder.genre);
        this.author = builder.author;
        this.publisher = builder.publisher;
        this.edition = builder.edition;
        this.pageCount = builder.pageCount;
    }

    public String getIsbn() {
        return getId();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    @Override
    public String getItemType() {
        return "Book";
    }

    @Override
    public String getContributor() {
        return author;
    }

    @Override
    public String getSummary() {
        return String.format("%-17s | %-34s | %-20s | %4d | %s",
                getIsbn(), truncate(getTitle(), 34), truncate(author, 20), getPublicationYear(),
                getGenre() == null ? "-" : getGenre().getLabel());
    }

    @Override
    public String getSearchableText() {
        return (super.getSearchableText() + " " + (publisher == null ? "" : publisher)).toLowerCase();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    /**
     * Builder for {@link Book}. Required fields are constructor arguments of the
     * builder itself, so an incomplete book cannot be built.
     */
    public static class Builder {

        private final String isbn;
        private final String title;
        private String author = "Unknown";
        private int publicationYear;
        private Genre genre = Genre.FICTION;
        private String publisher = "";
        private String edition = "1st";
        private int pageCount;

        /**
         * @param isbn  catalogue key, required
         * @param title book title, required
         */
        public Builder(String isbn, String title) {
            this.isbn = isbn;
            this.title = title;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder publicationYear(int publicationYear) {
            this.publicationYear = publicationYear;
            return this;
        }

        public Builder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder edition(String edition) {
            this.edition = edition;
            return this;
        }

        public Builder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }
}
