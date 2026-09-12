package com.library.domain.model;

import com.library.domain.enums.Genre;
import com.library.domain.enums.MembershipType;
import com.library.domain.enums.NotificationChannelType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A library member.
 *
 * <p>Holds its own borrowing history as an ordered list of catalogue keys, which
 * the recommendation strategies read. The history and the preference set are
 * exposed as unmodifiable views so nothing outside the patron can rewrite them
 * behind the service layer's back.</p>
 */
public class Patron {

    private final String patronId;
    private String name;
    private String email;
    private String phone;
    private MembershipType membershipType;
    private NotificationChannelType preferredChannel;
    private final LocalDate joinedOn;
    private final List<String> borrowingHistory = new ArrayList<>();
    private final Set<Genre> preferredGenres = new LinkedHashSet<>();
    private boolean active;

    public Patron(String patronId, String name, String email) {
        this(patronId, name, email, "", MembershipType.BASIC, NotificationChannelType.CONSOLE);
    }

    public Patron(String patronId, String name, String email, String phone,
                  MembershipType membershipType, NotificationChannelType preferredChannel) {
        this.patronId = patronId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.membershipType = membershipType;
        this.preferredChannel = preferredChannel;
        this.joinedOn = LocalDate.now();
        this.active = true;
    }

    public String getPatronId() {
        return patronId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = membershipType;
    }

    public NotificationChannelType getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(NotificationChannelType preferredChannel) {
        this.preferredChannel = preferredChannel;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @param itemId catalogue key of an item this patron has borrowed
     */
    public void recordBorrowing(String itemId) {
        borrowingHistory.add(itemId);
    }

    /**
     * @return the borrowing history in order, oldest first, as a read only view
     */
    public List<String> getBorrowingHistory() {
        return Collections.unmodifiableList(borrowingHistory);
    }

    /**
     * @return catalogue keys this patron has borrowed, without repeats
     */
    public Set<String> getDistinctBorrowedItems() {
        return new LinkedHashSet<>(borrowingHistory);
    }

    public void addPreferredGenre(Genre genre) {
        if (genre != null) {
            preferredGenres.add(genre);
        }
    }

    public Set<Genre> getPreferredGenres() {
        return Collections.unmodifiableSet(preferredGenres);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(patronId, ((Patron) other).patronId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patronId);
    }

    @Override
    public String toString() {
        return String.format("%-8s | %-22s | %-26s | %-8s | %-7s | %d borrowed",
                patronId, name, email, membershipType.getLabel(),
                active ? "ACTIVE" : "BLOCKED", borrowingHistory.size());
    }
}
