package com.library.domain.model;

import com.library.domain.enums.Genre;

import java.util.Objects;

/**
 * Abstraction over everything the library catalogues.
 *
 * <p>Holds only what every catalogued work genuinely shares. Anything specific
 * to a format lives on the subclass, so adding a new format never forces a
 * change here. This is the abstraction the catalogue, the search strategies and
 * the recommendation engine all program against.</p>
 */
public abstract class LibraryItem {

    private final String id;
    private String title;
    private int publicationYear;
    private Genre genre;

    protected LibraryItem(String id, String title, int publicationYear, Genre genre) {
        this.id = id;
        this.title = title;
        this.publicationYear = publicationYear;
        this.genre = genre;
    }

    /**
     * @return the catalogue key; an ISBN for a book, an ISSN issue for a magazine
     */
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    /**
     * @return a label such as Book or Magazine
     */
    public abstract String getItemType();

    /**
     * @return the person or body credited with the work
     */
    public abstract String getContributor();

    /**
     * @return one line describing the item, for listings
     */
    public abstract String getSummary();

    /**
     * The text a free-text search should look through. Declared here so every
     * format decides for itself what is worth searching, and the search
     * strategies never need to know which subclass they hold.
     *
     * @return lower case searchable text
     */
    public String getSearchableText() {
        return (title + " " + getContributor() + " " + id + " "
                + (genre == null ? "" : genre.getLabel())).toLowerCase();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(id, ((LibraryItem) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getClass().getSimpleName());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
