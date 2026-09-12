package com.library.service;

import com.library.domain.enums.MembershipType;
import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;

import java.util.List;

/**
 * Manages library members and their borrowing history.
 */
public interface PatronService {

    /**
     * @param name  the member's name
     * @param email a contact address
     * @return the registered member, with a generated identifier
     */
    Patron registerPatron(String name, String email);

    /**
     * @param name           the member's name
     * @param email          a contact address
     * @param phone          a contact number
     * @param membershipType the tier to grant
     * @param channel        how the member prefers to be contacted
     * @return the registered member
     */
    Patron registerPatron(String name, String email, String phone,
                          MembershipType membershipType, NotificationChannelType channel);

    /**
     * @param patronId who to change
     * @param name     new name, or {@code null} to keep the current one
     * @param email    new address, or {@code null} to keep the current one
     * @param phone    new number, or {@code null} to keep the current one
     * @return the updated member
     */
    Patron updatePatron(String patronId, String name, String email, String phone);

    /**
     * @param patronId the member
     * @return the member
     */
    Patron findById(String patronId);

    /**
     * @param patronId the member
     * @return the catalogue keys that member has borrowed, oldest first
     */
    List<String> getBorrowingHistory(String patronId);

    /**
     * @param patronId the member to suspend or restore
     * @param active   the membership state wanted
     * @return the updated member
     */
    Patron setActive(String patronId, boolean active);

    List<Patron> listAll();

    int getPatronCount();
}
