package com.library.repository.inmemory;

import com.library.domain.enums.CopyStatus;
import com.library.domain.model.ItemCopy;
import com.library.repository.CopyRepository;

import java.util.List;
import java.util.Optional;

/**
 * In-memory holdings register.
 */
public class InMemoryCopyRepository extends InMemoryRepository<ItemCopy, String>
        implements CopyRepository {

    public InMemoryCopyRepository() {
        super(ItemCopy::getBarcode);
    }

    @Override
    public List<ItemCopy> findByItemId(String itemId) {
        return filter(copy -> copy.getItemId().equals(itemId));
    }

    @Override
    public List<ItemCopy> findByBranch(String branchId) {
        return filter(copy -> copy.getBranchId().equals(branchId));
    }

    @Override
    public List<ItemCopy> findByItemAndStatus(String itemId, CopyStatus status) {
        return filter(copy -> copy.getItemId().equals(itemId) && copy.getStatus() == status);
    }

    @Override
    public Optional<ItemCopy> findFirstAvailable(String itemId, String branchId) {
        Optional<ItemCopy> atPreferredBranch = firstMatching(copy ->
                copy.getItemId().equals(itemId)
                        && copy.isAvailable()
                        && (branchId == null || copy.getBranchId().equals(branchId)));
        if (atPreferredBranch.isPresent() || branchId == null) {
            return atPreferredBranch;
        }
        return firstMatching(copy -> copy.getItemId().equals(itemId) && copy.isAvailable());
    }
}
