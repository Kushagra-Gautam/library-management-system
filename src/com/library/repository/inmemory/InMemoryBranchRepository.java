package com.library.repository.inmemory;

import com.library.domain.model.Branch;
import com.library.repository.BranchRepository;

/**
 * In-memory branch register.
 */
public class InMemoryBranchRepository extends InMemoryRepository<Branch, String>
        implements BranchRepository {

    public InMemoryBranchRepository() {
        super(Branch::getBranchId);
    }
}
