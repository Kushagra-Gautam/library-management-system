package com.library.service.impl;

import com.library.domain.enums.CopyStatus;
import com.library.domain.enums.EventType;
import com.library.domain.model.Branch;
import com.library.domain.model.ItemCopy;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.ItemNotFoundException;
import com.library.exception.TransferException;
import com.library.repository.BranchRepository;
import com.library.repository.CopyRepository;
import com.library.service.TransferService;
import com.library.util.LoggingConfig;
import com.library.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Moves copies between branches in two steps.
 *
 * <p>A transfer is not instantaneous: the copy is marked {@code IN_TRANSIT} when
 * it leaves and only becomes lendable again when it is booked in at the far end.
 * Modelling the journey as a state rather than a straight reassignment is what
 * stops a copy being lent from a branch that does not physically hold it yet.</p>
 */
public class DefaultTransferService implements TransferService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultTransferService.class);

    private final CopyRepository copyRepository;
    private final BranchRepository branchRepository;
    private final EventPublisher eventPublisher;
    private final Map<String, String> destinations = new LinkedHashMap<>();

    public DefaultTransferService(CopyRepository copyRepository, BranchRepository branchRepository,
                                  EventPublisher eventPublisher) {
        this.copyRepository = copyRepository;
        this.branchRepository = branchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Branch addBranch(String branchId, String name, String city) {
        Branch branch = new Branch(Validator.requireText(branchId, "branch id"),
                Validator.requireName(name, "branch name"),
                Validator.requireText(city, "city"));
        branchRepository.save(branch);
        LOGGER.info(() -> "Registered branch " + branch.getBranchId() + " (" + name + ")");
        return branch;
    }

    @Override
    public ItemCopy initiateTransfer(String barcode, String toBranchId) {
        ItemCopy copy = copyRepository.findById(Validator.requireText(barcode, "barcode"))
                .orElseThrow(() -> ItemNotFoundException.forBarcode(barcode));
        Branch destination = findBranch(toBranchId);

        if (copy.getBranchId().equals(destination.getBranchId())) {
            throw new TransferException("Copy " + barcode + " is already held at "
                    + destination.getBranchId() + ".");
        }
        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new TransferException("Copy " + barcode + " is "
                    + copy.getStatus().getLabel().toLowerCase() + " and cannot be moved.");
        }

        String origin = copy.getBranchId();
        copy.setStatus(CopyStatus.IN_TRANSIT);
        copyRepository.save(copy);
        destinations.put(barcode, destination.getBranchId());

        LOGGER.info(() -> String.format("Copy %s dispatched from %s to %s",
                barcode, origin, destination.getBranchId()));
        return copy;
    }

    @Override
    public ItemCopy completeTransfer(String barcode) {
        ItemCopy copy = copyRepository.findById(Validator.requireText(barcode, "barcode"))
                .orElseThrow(() -> ItemNotFoundException.forBarcode(barcode));
        if (copy.getStatus() != CopyStatus.IN_TRANSIT) {
            throw new TransferException("Copy " + barcode + " is not in transit.");
        }
        String destination = destinations.remove(barcode);
        if (destination == null) {
            throw new TransferException("No destination is recorded for copy " + barcode + ".");
        }

        String origin = copy.getBranchId();
        copy.setBranchId(destination);
        copy.setStatus(CopyStatus.AVAILABLE);
        copyRepository.save(copy);

        LOGGER.info(() -> String.format("Copy %s received at %s", barcode, destination));
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("from", origin);
        payload.put("to", destination);
        eventPublisher.publish(new LibraryEvent(EventType.COPY_TRANSFERRED, copy.getItemId(), null,
                String.format("Copy %s moved from %s to %s", barcode, origin, destination), payload));
        return copy;
    }

    @Override
    public List<ItemCopy> listInTransit() {
        return copyRepository.findAll().stream()
                .filter(copy -> copy.getStatus() == CopyStatus.IN_TRANSIT)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Branch> listBranches() {
        return branchRepository.findAll();
    }

    @Override
    public Branch findBranch(String branchId) {
        return branchRepository.findById(Validator.requireText(branchId, "branch id"))
                .orElseThrow(() -> new ItemNotFoundException("branch " + branchId));
    }
}
