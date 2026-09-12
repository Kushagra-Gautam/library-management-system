package com.library.service;

import com.library.domain.model.Branch;
import com.library.domain.model.ItemCopy;

import java.util.List;

/**
 * Moves copies between branches.
 */
public interface TransferService {

    /**
     * @param branchId the identifier to use
     * @param name     the branch name
     * @param city     where it is
     * @return the registered branch
     */
    Branch addBranch(String branchId, String name, String city);

    /**
     * Sends a copy to another branch. The copy is marked in transit so it cannot
     * be lent while it is on the road.
     *
     * @param barcode      the copy to move
     * @param toBranchId   where it is going
     * @return the copy, now in transit
     */
    ItemCopy initiateTransfer(String barcode, String toBranchId);

    /**
     * Books a copy in at its destination and makes it lendable again.
     *
     * @param barcode the copy arriving
     * @return the copy, now available
     */
    ItemCopy completeTransfer(String barcode);

    /**
     * @return every copy currently in transit
     */
    List<ItemCopy> listInTransit();

    List<Branch> listBranches();

    Branch findBranch(String branchId);
}
