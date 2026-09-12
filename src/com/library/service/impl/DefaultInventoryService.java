package com.library.service.impl;

import com.library.domain.enums.CopyStatus;
import com.library.domain.enums.EventType;
import com.library.domain.model.ItemCopy;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.ItemNotFoundException;
import com.library.repository.BranchRepository;
import com.library.repository.CopyRepository;
import com.library.repository.ItemRepository;
import com.library.service.InventoryService;
import com.library.util.IdGenerator;
import com.library.util.LoggingConfig;
import com.library.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Default holdings implementation.
 *
 * <p>Counts are derived from copy statuses rather than kept as separate totals,
 * so an available count can never drift out of step with the copies themselves.</p>
 */
public class DefaultInventoryService implements InventoryService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultInventoryService.class);

    private final CopyRepository copyRepository;
    private final ItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final EventPublisher eventPublisher;

    public DefaultInventoryService(CopyRepository copyRepository, ItemRepository itemRepository,
                                   BranchRepository branchRepository, EventPublisher eventPublisher) {
        this.copyRepository = copyRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ItemCopy addCopy(String itemId, String branchId) {
        String validItemId = Validator.requireText(itemId, "item id");
        String validBranchId = Validator.requireText(branchId, "branch id");
        if (!itemRepository.existsById(validItemId)) {
            throw new ItemNotFoundException(validItemId);
        }
        if (!branchRepository.existsById(validBranchId)) {
            throw new ItemNotFoundException("branch " + validBranchId);
        }
        ItemCopy copy = new ItemCopy(IdGenerator.nextBarcode(), validItemId, validBranchId);
        copyRepository.save(copy);
        LOGGER.info(() -> "Added copy " + copy.getBarcode() + " of " + validItemId
                + " at " + validBranchId);
        eventPublisher.publish(new LibraryEvent(EventType.COPY_ADDED, validItemId, null,
                "Copy " + copy.getBarcode() + " added at " + validBranchId));
        return copy;
    }

    @Override
    public List<ItemCopy> addCopies(String itemId, String branchId, int quantity) {
        int count = Validator.requirePositive(quantity, "quantity");
        List<ItemCopy> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            created.add(addCopy(itemId, branchId));
        }
        return created;
    }

    @Override
    public boolean removeCopy(String barcode) {
        ItemCopy copy = findByBarcode(barcode);
        if (copy.getStatus() == CopyStatus.BORROWED) {
            throw new com.library.exception.InvalidInputException(
                    "Copy " + barcode + " is on loan and cannot be withdrawn.");
        }
        LOGGER.info(() -> "Withdrew copy " + barcode);
        return copyRepository.deleteById(barcode);
    }

    @Override
    public ItemCopy findByBarcode(String barcode) {
        return copyRepository.findById(Validator.requireText(barcode, "barcode"))
                .orElseThrow(() -> ItemNotFoundException.forBarcode(barcode));
    }

    @Override
    public int getAvailableCount(String itemId) {
        return copyRepository.findByItemAndStatus(itemId, CopyStatus.AVAILABLE).size();
    }

    @Override
    public int getBorrowedCount(String itemId) {
        return copyRepository.findByItemAndStatus(itemId, CopyStatus.BORROWED).size();
    }

    @Override
    public int getTotalCount(String itemId) {
        return copyRepository.findByItemId(itemId).size();
    }

    @Override
    public boolean isAvailable(String itemId) {
        return getAvailableCount(itemId) > 0;
    }

    @Override
    public List<ItemCopy> listByBranch(String branchId) {
        return copyRepository.findByBranch(branchId);
    }

    @Override
    public List<ItemCopy> listByItem(String itemId) {
        return copyRepository.findByItemId(itemId);
    }

    @Override
    public Map<String, Long> countByBranch() {
        return copyRepository.findAll().stream()
                .collect(Collectors.groupingBy(ItemCopy::getBranchId, Collectors.counting()));
    }
}
