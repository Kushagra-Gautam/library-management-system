package com.library.service;

import com.library.domain.model.ItemCopy;

import java.util.List;
import java.util.Map;

/**
 * Tracks physical copies: how many exist, where they are, and which are free.
 */
public interface InventoryService {

    /**
     * @param itemId   the work being stocked
     * @param branchId where the copy is held
     * @return the new copy, with a generated barcode
     */
    ItemCopy addCopy(String itemId, String branchId);

    /**
     * @param itemId   the work being stocked
     * @param branchId where the copies are held
     * @param quantity how many to add
     * @return the new copies
     */
    List<ItemCopy> addCopies(String itemId, String branchId, int quantity);

    /**
     * @param barcode the copy to withdraw
     * @return {@code true} when it was removed
     */
    boolean removeCopy(String barcode);

    /**
     * @param barcode the copy wanted
     * @return the copy
     */
    ItemCopy findByBarcode(String barcode);

    /**
     * @param itemId the work
     * @return how many copies are free to lend across all branches
     */
    int getAvailableCount(String itemId);

    /**
     * @param itemId the work
     * @return how many copies are currently on loan
     */
    int getBorrowedCount(String itemId);

    /**
     * @param itemId the work
     * @return how many copies exist in total
     */
    int getTotalCount(String itemId);

    /**
     * @param itemId the work
     * @return {@code true} when at least one copy can be lent right now
     */
    boolean isAvailable(String itemId);

    /**
     * @param branchId the branch
     * @return every copy held there
     */
    List<ItemCopy> listByBranch(String branchId);

    /**
     * @param itemId the work
     * @return every copy of it
     */
    List<ItemCopy> listByItem(String itemId);

    /**
     * @return branch identifier to number of copies held
     */
    Map<String, Long> countByBranch();
}
