package com.library.repository;

import com.library.domain.enums.CopyStatus;
import com.library.domain.model.ItemCopy;

import java.util.List;
import java.util.Optional;

/**
 * Storage for physical copies, keyed by barcode.
 */
public interface CopyRepository extends Repository<ItemCopy, String> {

    /**
     * @param itemId the catalogue key
     * @return every copy of that work, in any branch
     */
    List<ItemCopy> findByItemId(String itemId);

    /**
     * @param branchId the branch
     * @return every copy currently held at that branch
     */
    List<ItemCopy> findByBranch(String branchId);

    /**
     * @param itemId the catalogue key
     * @param status the status wanted
     * @return copies of that work in that status
     */
    List<ItemCopy> findByItemAndStatus(String itemId, CopyStatus status);

    /**
     * @param itemId   the catalogue key
     * @param branchId the branch to prefer, or {@code null} for any branch
     * @return the first lendable copy found
     */
    Optional<ItemCopy> findFirstAvailable(String itemId, String branchId);
}
