package com.library.domain.model;

import com.library.domain.enums.Genre;

/**
 * A catalogued magazine issue.
 *
 * <p>Present to prove the catalogue really is open to new formats: it is stored,
 * searched, lent and returned by exactly the same code paths as a {@link Book},
 * because everything downstream depends on {@link LibraryItem} rather than on
 * {@code Book}.</p>
 */
public class Magazine extends LibraryItem {

    private String publisher;
    private int issueNumber;

    public Magazine(String issn, String title, int publicationYear, Genre genre,
                    String publisher, int issueNumber) {
        super(issn, title, publicationYear, genre);
        this.publisher = publisher;
        this.issueNumber = issueNumber;
    }

    public String getIssn() {
        return getId();
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }

    @Override
    public String getItemType() {
        return "Magazine";
    }

    @Override
    public String getContributor() {
        return publisher;
    }

    @Override
    public String getSummary() {
        return String.format("%-17s | %-34s | %-20s | %4d | issue %d",
                getId(), getTitle(), publisher, getPublicationYear(), issueNumber);
    }
}
