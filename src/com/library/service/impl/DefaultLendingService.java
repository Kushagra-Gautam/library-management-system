package com.library.service.impl;

import com.library.domain.enums.CopyStatus;
import com.library.domain.enums.EventType;
import com.library.domain.enums.LoanStatus;
import com.library.domain.model.ItemCopy;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Loan;
import com.library.domain.model.Patron;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.BorrowingLimitExceededException;
import com.library.exception.CopyNotAvailableException;
import com.library.exception.InvalidInputException;
import com.library.exception.ItemNotFoundException;
import com.library.policy.LoanPolicy;
import com.library.policy.LoanPolicyResolver;
import com.library.repository.CopyRepository;
import com.library.repository.ItemRepository;
import com.library.repository.LoanRepository;
import com.library.service.LendingService;
import com.library.service.PatronService;
import com.library.service.ReservationService;
import com.library.util.IdGenerator;
import com.library.util.LoggingConfig;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The checkout and return process.
 *
 * <p>Every collaborator arrives through the constructor as an interface, which
 * is what lets the test harness drive this class against in-memory stores with
 * no wiring changes. The lending rules themselves are not written here: the
 * loan period, the borrowing limit and the fine rate all come from the
 * {@link LoanPolicy} resolved for the patron's tier.</p>
 *
 * <p>Returning a copy publishes an event rather than calling the notification
 * code directly, so this class stays unaware of reservations and messaging even
 * though both react to a return.</p>
 */
public class DefaultLendingService implements LendingService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultLendingService.class);

    private final LoanRepository loanRepository;
    private final CopyRepository copyRepository;
    private final ItemRepository itemRepository;
    private final PatronService patronService;
    private final LoanPolicyResolver policyResolver;
    private final EventPublisher eventPublisher;
    private ReservationService reservationService;

    public DefaultLendingService(LoanRepository loanRepository, CopyRepository copyRepository,
                                 ItemRepository itemRepository, PatronService patronService,
                                 LoanPolicyResolver policyResolver, EventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.copyRepository = copyRepository;
        this.itemRepository = itemRepository;
        this.patronService = patronService;
        this.policyResolver = policyResolver;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Supplied after construction because lending and reservations refer to one
     * another. Keeping it optional means lending works perfectly well without the
     * reservation module present at all.
     *
     * @param reservationService the hold queue, or {@code null} to disable holds
     */
    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public Loan checkout(String patronId, String itemId, String branchId) {
        Patron patron = patronService.findById(patronId);
        LibraryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        ItemCopy copy = copyRepository.findFirstAvailable(itemId, branchId)
                .orElseThrow(() -> {
                    LOGGER.warning(() -> "No copy of " + itemId + " available for " + patronId);
                    return new CopyNotAvailableException(itemId);
                });
        return lend(patron, copy, item);
    }

    @Override
    public Loan checkoutCopy(String patronId, String barcode) {
        Patron patron = patronService.findById(patronId);
        ItemCopy copy = copyRepository.findById(barcode)
                .orElseThrow(() -> ItemNotFoundException.forBarcode(barcode));
        if (!copy.isAvailable()) {
            throw new CopyNotAvailableException(copy.getItemId(),
                    "Copy " + barcode + " is " + copy.getStatus().getLabel().toLowerCase() + ".");
        }
        LibraryItem item = itemRepository.findById(copy.getItemId())
                .orElseThrow(() -> new ItemNotFoundException(copy.getItemId()));
        return lend(patron, copy, item);
    }

    private Loan lend(Patron patron, ItemCopy copy, LibraryItem item) {
        if (!patron.isActive()) {
            throw new InvalidInputException(
                    "Patron " + patron.getPatronId() + " is blocked and cannot borrow.");
        }
        LoanPolicy policy = policyResolver.resolve(patron.getMembershipType());
        int held = loanRepository.findActiveByPatron(patron.getPatronId()).size();
        if (held >= policy.getMaxConcurrentLoans()) {
            LOGGER.warning(() -> "Borrowing limit reached by " + patron.getPatronId());
            throw new BorrowingLimitExceededException(patron.getPatronId(),
                    policy.getMaxConcurrentLoans());
        }

        LocalDate today = LocalDate.now();
        Loan loan = new Loan(IdGenerator.nextLoanId(), copy.getBarcode(), copy.getItemId(),
                patron.getPatronId(), copy.getBranchId(), today,
                today.plusDays(policy.getLoanPeriodDays()));
        loanRepository.save(loan);

        copy.setStatus(CopyStatus.BORROWED);
        copyRepository.save(copy);
        patron.recordBorrowing(copy.getItemId());

        LOGGER.info(() -> String.format("Loan %s: copy %s of '%s' to %s, due %s",
                loan.getLoanId(), copy.getBarcode(), item.getTitle(),
                patron.getPatronId(), loan.getDueOn()));

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("barcode", copy.getBarcode());
        payload.put("dueOn", loan.getDueOn().toString());
        eventPublisher.publish(new LibraryEvent(EventType.BOOK_BORROWED, copy.getItemId(),
                patron.getPatronId(),
                "'" + item.getTitle() + "' is due back on " + loan.getDueOn(), payload));
        return loan;
    }

    @Override
    public Loan returnCopy(String barcode) {
        ItemCopy copy = copyRepository.findById(barcode)
                .orElseThrow(() -> ItemNotFoundException.forBarcode(barcode));
        Loan loan = loanRepository.findActiveByBarcode(barcode)
                .orElseThrow(() -> new InvalidInputException(
                        "Copy " + barcode + " is not currently on loan."));

        LocalDate today = LocalDate.now();
        loan.setReturnedOn(today);
        loan.setStatus(LoanStatus.RETURNED);

        Patron patron = patronService.findById(loan.getPatronId());
        LoanPolicy policy = policyResolver.resolve(patron.getMembershipType());
        long overdueDays = loan.daysOverdue(today);
        double fine = policy.calculateFine(overdueDays);
        loan.setFineCharged(fine);
        loanRepository.save(loan);

        copy.setStatus(CopyStatus.AVAILABLE);
        copyRepository.save(copy);

        String title = itemRepository.findById(copy.getItemId())
                .map(LibraryItem::getTitle).orElse(copy.getItemId());
        LOGGER.info(() -> String.format("Returned copy %s (loan %s)%s",
                barcode, loan.getLoanId(),
                fine > 0 ? String.format(", fine %.2f for %d day(s)", fine, overdueDays) : ""));

        eventPublisher.publish(new LibraryEvent(EventType.BOOK_RETURNED, copy.getItemId(),
                loan.getPatronId(), "'" + title + "' was returned"));

        if (reservationService != null) {
            reservationService.fulfilNext(copy.getItemId(), barcode);
        }
        return loan;
    }

    @Override
    public Loan renew(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new InvalidInputException("No loan exists with ID " + loanId));
        if (!loan.isActive()) {
            throw new InvalidInputException("Loan " + loanId + " has already been closed.");
        }
        if (reservationService != null && !reservationService.getQueue(loan.getItemId()).isEmpty()) {
            throw new InvalidInputException(
                    "Another patron is waiting for this title, so it cannot be renewed.");
        }
        Patron patron = patronService.findById(loan.getPatronId());
        LoanPolicy policy = policyResolver.resolve(patron.getMembershipType());
        loan.setDueOn(loan.getDueOn().plusDays(policy.getLoanPeriodDays()));
        loanRepository.save(loan);
        LOGGER.info(() -> "Renewed loan " + loanId + " to " + loan.getDueOn());
        return loan;
    }

    @Override
    public List<Loan> getActiveLoans(String patronId) {
        patronService.findById(patronId);
        return loanRepository.findActiveByPatron(patronId);
    }

    @Override
    public List<Loan> findOverdueLoans() {
        LocalDate today = LocalDate.now();
        return loanRepository.findAllActive().stream()
                .filter(loan -> loan.isOverdue(today))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public int flagOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<Loan> overdue = findOverdueLoans();
        for (Loan loan : overdue) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
            String title = itemRepository.findById(loan.getItemId())
                    .map(LibraryItem::getTitle).orElse(loan.getItemId());
            eventPublisher.publish(new LibraryEvent(EventType.BOOK_OVERDUE, loan.getItemId(),
                    loan.getPatronId(),
                    String.format("'%s' was due on %s and is %d day(s) late",
                            title, loan.getDueOn(), loan.daysOverdue(today))));
        }
        LOGGER.info(() -> "Flagged " + overdue.size() + " overdue loan(s)");
        return overdue.size();
    }

    @Override
    public double calculateOutstandingFine(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new InvalidInputException("No loan exists with ID " + loanId));
        Patron patron = patronService.findById(loan.getPatronId());
        LoanPolicy policy = policyResolver.resolve(patron.getMembershipType());
        return policy.calculateFine(loan.daysOverdue(LocalDate.now()));
    }
}
