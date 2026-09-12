package com.library.test;

import com.library.app.LibraryApplication;
import com.library.domain.enums.CopyStatus;
import com.library.domain.enums.Genre;
import com.library.domain.enums.MembershipType;
import com.library.domain.enums.NotificationChannelType;
import com.library.domain.enums.ReservationStatus;
import com.library.domain.model.Book;
import com.library.domain.model.ItemCopy;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Loan;
import com.library.domain.model.Magazine;
import com.library.domain.model.Patron;
import com.library.domain.model.Reservation;
import com.library.event.LibraryEvent;
import com.library.exception.BorrowingLimitExceededException;
import com.library.exception.CopyNotAvailableException;
import com.library.exception.DuplicateItemException;
import com.library.exception.InvalidInputException;
import com.library.exception.ItemNotFoundException;
import com.library.exception.LibraryException;
import com.library.exception.PatronNotFoundException;
import com.library.exception.ReservationException;
import com.library.exception.TransferException;
import com.library.factory.LibraryItemFactory;
import com.library.policy.BasicLoanPolicy;
import com.library.policy.LoanPolicy;
import com.library.policy.PremiumLoanPolicy;
import com.library.policy.StudentLoanPolicy;
import com.library.recommendation.RecommendationStrategy;
import com.library.search.AuthorSearchStrategy;
import com.library.search.CompositeSearchStrategy;
import com.library.search.IsbnSearchStrategy;
import com.library.search.SearchStrategy;
import com.library.search.TitleSearchStrategy;
import com.library.util.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand written test harness.
 *
 * <p>No external framework is used, in keeping with the brief's no-dependencies
 * constraint. Each group builds a fresh {@link LibraryApplication}, so tests
 * never share state — which is only possible because every service takes its
 * collaborators through a constructor rather than reaching for a global.</p>
 */
public class LibraryTestRunner {

    private static final String LINE =
            "================================================================================";

    private int passed;
    private int failed;
    private final List<String> failures = new ArrayList<>();

    /**
     * Allows the suite to be launched on its own.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        com.library.util.LoggingConfig.configure(java.util.logging.Level.OFF);
        new LibraryTestRunner().runAll();
    }

    /**
     * @return {@code true} when every check passed
     */
    public boolean runAll() {
        passed = 0;
        failed = 0;
        failures.clear();

        System.out.println();
        System.out.println(LINE);
        System.out.println("  LIBRARY MANAGEMENT SYSTEM - TEST SUITE");
        System.out.println(LINE);

        testValidation();
        testBookBuilder();
        testFactory();
        testCatalogueCrud();
        testSearchStrategies();
        testPatronManagement();
        testInventoryCounts();
        testLendingHappyPath();
        testLendingRules();
        testLoanPolicies();
        testFinesAndOverdue();
        testReservationQueue();
        testReservationNotification();
        testMultiBranchTransfer();
        testRecommendations();
        testObserverBus();
        testPolymorphismAndLsp();

        summary();
        return failed == 0;
    }

    private void testValidation() {
        section("Validation");
        check("A valid ISBN-13 passes", "9780132350884".equals(Validator.requireIsbn("9780132350884")));
        check("Hyphens are tolerated in an ISBN",
                "978-0132350884".equals(Validator.requireIsbn("978-0132350884")));
        check("A short ISBN is rejected", throwsInvalid(() -> Validator.requireIsbn("12345")));
        check("A non-numeric ISBN is rejected", throwsInvalid(() -> Validator.requireIsbn("97801323508AB")));
        check("A blank name is rejected", throwsInvalid(() -> Validator.requireName("", "title")));
        check("A malformed email is rejected", throwsInvalid(() -> Validator.requireEmail("nope")));
        check("An impossible year is rejected", throwsInvalid(() -> Validator.requireYear(1200)));
        check("An unparseable number is rejected", throwsInvalid(() -> Validator.parseInt("ten", "count")));
        check("The parse failure is kept as the cause", causeOf(() -> Validator.parseInt("ten", "count"))
                instanceof NumberFormatException);
    }

    private void testBookBuilder() {
        section("Builder pattern");
        Book book = new Book.Builder("9780134685991", "Effective Java")
                .author("Joshua Bloch")
                .publicationYear(2018)
                .genre(Genre.TECHNOLOGY)
                .pageCount(412)
                .build();
        check("Required fields are set", "Effective Java".equals(book.getTitle()));
        check("Optional fields are set", book.getPageCount() == 412);
        check("Unset fields take a default", "1st".equals(book.getEdition()));
        Book minimal = new Book.Builder("9780140449136", "The Odyssey").build();
        check("A book builds with only the required fields", "Unknown".equals(minimal.getAuthor()));
        check("The ISBN is the catalogue key", "9780140449136".equals(minimal.getId()));
    }

    private void testFactory() {
        section("Factory pattern");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("isbn", "9780132350884");
        attributes.put("title", "Clean Code");
        attributes.put("author", "Robert C. Martin");
        attributes.put("year", "2008");
        attributes.put("genre", "Technology");
        LibraryItem book = LibraryItemFactory.create("book", attributes);
        check("The factory builds a book", book instanceof Book);
        check("The factory applies the genre", book.getGenre() == Genre.TECHNOLOGY);

        Map<String, String> magazineAttributes = new HashMap<>();
        magazineAttributes.put("isbn", "ISSN-2049-3630");
        magazineAttributes.put("title", "Java Magazine");
        magazineAttributes.put("publisher", "Oracle");
        magazineAttributes.put("year", "2024");
        magazineAttributes.put("issue", "112");
        LibraryItem magazine = LibraryItemFactory.create("magazine", magazineAttributes);
        check("The factory builds a magazine", magazine instanceof Magazine);
        check("An unknown format is rejected",
                throwsInvalid(() -> LibraryItemFactory.create("papyrus", attributes)));
        check("The factory validates as it builds", throwsInvalid(() -> {
            Map<String, String> bad = new HashMap<>(attributes);
            bad.put("isbn", "nope");
            LibraryItemFactory.create("book", bad);
        }));
    }

    private void testCatalogueCrud() {
        section("Catalogue management");
        LibraryApplication app = freshApp();
        app.getCatalogService().addBook("9780132350884", "Clean Code",
                "Robert C. Martin", 2008, Genre.TECHNOLOGY);
        check("A book is catalogued", app.getCatalogService().getCatalogueSize() == 1);
        check("It can be found by key",
                "Clean Code".equals(app.getCatalogService().findById("9780132350884").getTitle()));
        check("A duplicate key is rejected", throwsType(DuplicateItemException.class,
                () -> app.getCatalogService().addBook("9780132350884", "Clean Code Again",
                        "Someone Else", 2010, Genre.TECHNOLOGY)));
        app.getCatalogService().updateItem("9780132350884", "Clean Code, 2nd edition", 2012, null);
        check("An update changes the title", app.getCatalogService()
                .findById("9780132350884").getTitle().contains("2nd edition"));
        check("An update leaves untouched fields alone",
                app.getCatalogService().findById("9780132350884").getGenre() == Genre.TECHNOLOGY);
        app.getCatalogService().removeItem("9780132350884");
        check("A record can be withdrawn", app.getCatalogService().getCatalogueSize() == 0);
        check("A missing record raises the right exception", throwsType(ItemNotFoundException.class,
                () -> app.getCatalogService().findById("9780132350884")));
    }

    private void testSearchStrategies() {
        section("Search (strategy pattern)");
        LibraryApplication app = freshApp();
        app.getCatalogService().addBook("9780132350884", "Clean Code",
                "Robert C. Martin", 2008, Genre.TECHNOLOGY);
        app.getCatalogService().addBook("9780134494166", "Clean Architecture",
                "Robert C. Martin", 2017, Genre.TECHNOLOGY);
        app.getCatalogService().addBook("9780345391803", "Hitchhiker's Guide",
                "Douglas Adams", 1979, Genre.FANTASY);

        check("Title search matches partially",
                app.getCatalogService().search(new TitleSearchStrategy(), "clean").size() == 2);
        check("Title search ignores case",
                app.getCatalogService().search(new TitleSearchStrategy(), "CLEAN").size() == 2);
        check("Author search finds both titles by one author",
                app.getCatalogService().search(new AuthorSearchStrategy(), "Martin").size() == 2);
        check("ISBN search finds one record",
                app.getCatalogService().search(new IsbnSearchStrategy(), "9780345391803").size() == 1);
        check("ISBN search ignores hyphens",
                app.getCatalogService().search(new IsbnSearchStrategy(), "978-0345391803").size() == 1);

        SearchStrategy composite = new CompositeSearchStrategy(
                new TitleSearchStrategy(), new AuthorSearchStrategy());
        check("The composite unions results without duplicates",
                app.getCatalogService().search(composite, "clean").size() == 2);
        check("A composite is itself a strategy", composite instanceof SearchStrategy);
        check("An empty query returns nothing",
                app.getCatalogService().search(new TitleSearchStrategy(), "").isEmpty());
    }

    private void testPatronManagement() {
        section("Patron management");
        LibraryApplication app = freshApp();
        Patron patron = app.getPatronService().registerPatron("Aarav Sharma", "aarav@example.com");
        check("A patron gets a generated identifier", patron.getPatronId().startsWith("P-"));
        check("A patron starts active", patron.isActive());
        check("A patron starts with no history", patron.getBorrowingHistory().isEmpty());
        check("A duplicate email is rejected", throwsType(DuplicateItemException.class,
                () -> app.getPatronService().registerPatron("Impostor", "aarav@example.com")));
        check("An unknown patron raises the right exception", throwsType(PatronNotFoundException.class,
                () -> app.getPatronService().findById("P-999")));
        app.getPatronService().updatePatron(patron.getPatronId(), null, "new@example.com", null);
        check("An update changes only what was supplied",
                "new@example.com".equals(patron.getEmail()) && "Aarav Sharma".equals(patron.getName()));
        check("History is exposed read only", throwsType(UnsupportedOperationException.class,
                () -> patron.getBorrowingHistory().add("tampered")));
        app.getPatronService().setActive(patron.getPatronId(), false);
        check("A patron can be blocked", !patron.isActive());
    }

    private void testInventoryCounts() {
        section("Inventory");
        LibraryApplication app = seededApp();
        check("Copies are counted", app.getInventoryService().getTotalCount("9780132350884") == 2);
        check("All copies start available",
                app.getInventoryService().getAvailableCount("9780132350884") == 2);
        check("Nothing is on loan yet",
                app.getInventoryService().getBorrowedCount("9780132350884") == 0);
        check("The title reports as available",
                app.getInventoryService().isAvailable("9780132350884"));

        app.getLendingService().checkout("P-001", "9780132350884", "BR-DEL");
        check("Borrowing decrements availability",
                app.getInventoryService().getAvailableCount("9780132350884") == 1);
        check("Borrowing increments the loaned count",
                app.getInventoryService().getBorrowedCount("9780132350884") == 1);
        check("The total is unchanged by lending",
                app.getInventoryService().getTotalCount("9780132350884") == 2);
        check("Copies are counted per branch",
                app.getInventoryService().countByBranch().get("BR-DEL") >= 1);
        check("A copy cannot be stocked for an uncatalogued work",
                throwsType(ItemNotFoundException.class,
                        () -> app.getInventoryService().addCopy("0000000000000", "BR-DEL")));
    }

    private void testLendingHappyPath() {
        section("Lending");
        LibraryApplication app = seededApp();
        Loan loan = app.getLendingService().checkout("P-001", "9780132350884", "BR-DEL");
        check("A loan is created", loan.getLoanId().startsWith("L-"));
        check("The loan is active", loan.isActive());
        check("A due date is set", loan.getDueOn().isAfter(loan.getBorrowedOn()));
        check("The copy is marked borrowed", app.getInventoryService()
                .findByBarcode(loan.getBarcode()).getStatus() == CopyStatus.BORROWED);
        check("The patron's history records it", app.getPatronService()
                .getBorrowingHistory("P-001").contains("9780132350884"));
        check("The loan appears in the patron's active loans",
                app.getLendingService().getActiveLoans("P-001").size() == 1);

        app.getLendingService().returnCopy(loan.getBarcode());
        check("Returning closes the loan", !loan.isActive());
        check("Returning records the date", loan.getReturnedOn() != null);
        check("The copy is available again", app.getInventoryService()
                .findByBarcode(loan.getBarcode()).getStatus() == CopyStatus.AVAILABLE);
        check("The patron has no active loans left",
                app.getLendingService().getActiveLoans("P-001").isEmpty());
        check("History survives the return", app.getPatronService()
                .getBorrowingHistory("P-001").contains("9780132350884"));
    }

    private void testLendingRules() {
        section("Lending rules");
        LibraryApplication app = seededApp();
        check("Lending an uncatalogued work is refused", throwsType(ItemNotFoundException.class,
                () -> app.getLendingService().checkout("P-001", "0000000000000", null)));
        check("Lending to an unknown patron is refused", throwsType(PatronNotFoundException.class,
                () -> app.getLendingService().checkout("P-999", "9780132350884", null)));

        Loan onlyCopy = app.getLendingService().checkout("P-001", "9780134685991", "BR-DEL");
        check("When no copy is free, checkout is refused",
                throwsType(CopyNotAvailableException.class,
                        () -> app.getLendingService().checkout("P-002", "9780134685991", null)));
        check("Returning a copy that is not out is refused", throwsInvalid(
                () -> app.getLendingService().returnCopy(
                        app.getInventoryService().listByItem("9780132350884").get(0).getBarcode())));

        app.getPatronService().setActive("P-003", false);
        check("A blocked patron cannot borrow", throwsInvalid(
                () -> app.getLendingService().checkout("P-003", "9780132350884", null)));
        app.getPatronService().setActive("P-003", true);

        app.getLendingService().returnCopy(onlyCopy.getBarcode());
        check("A returned copy can be lent again",
                app.getLendingService().checkout("P-002", "9780134685991", null) != null);
    }

    private void testLoanPolicies() {
        section("Loan policies (strategy pattern)");
        LoanPolicy basic = new BasicLoanPolicy();
        LoanPolicy student = new StudentLoanPolicy();
        LoanPolicy premium = new PremiumLoanPolicy();
        check("Basic terms run 14 days", basic.getLoanPeriodDays() == 14);
        check("Students get longer loans", student.getLoanPeriodDays() > basic.getLoanPeriodDays());
        check("Premium members may hold the most books",
                premium.getMaxConcurrentLoans() > student.getMaxConcurrentLoans());
        check("The default fine is per day", basic.calculateFine(3) == 3 * basic.getFinePerDay());
        check("Premium overrides the default with a grace period", premium.calculateFine(2) == 0.0);
        check("Premium charges after the grace period expires", premium.calculateFine(5) > 0);
        check("No fine is charged when nothing is overdue", student.calculateFine(0) == 0.0);

        LibraryApplication app = seededApp();
        check("The resolver maps a tier to its policy", "Student".equals(app.getPolicyResolver()
                .resolve(MembershipType.STUDENT).getPolicyName()));

        LibraryApplication limited = seededApp();
        limited.getLendingService().checkout("P-001", "9780132350884", null);
        limited.getLendingService().checkout("P-001", "9780201633610", null);
        limited.getLendingService().checkout("P-001", "9780345391803", null);
        check("The borrowing limit is enforced from the policy",
                throwsType(BorrowingLimitExceededException.class,
                        () -> limited.getLendingService().checkout("P-001", "9780062316110", null)));
    }

    private void testFinesAndOverdue() {
        section("Fines and overdue loans");
        LibraryApplication app = seededApp();
        Loan loan = app.getLendingService().checkout("P-001", "9780132350884", null);
        check("A loan on time is not overdue", !loan.isOverdue(java.time.LocalDate.now()));
        check("Days overdue is zero when on time", loan.daysOverdue(java.time.LocalDate.now()) == 0);

        loan.setDueOn(java.time.LocalDate.now().minusDays(4));
        check("A past due date makes the loan overdue", loan.isOverdue(java.time.LocalDate.now()));
        check("Days overdue counts correctly", loan.daysOverdue(java.time.LocalDate.now()) == 4);
        check("The service finds overdue loans",
                app.getLendingService().findOverdueLoans().size() == 1);
        check("An outstanding fine is calculated",
                app.getLendingService().calculateOutstandingFine(loan.getLoanId()) > 0);
        check("Flagging overdue loans reports the count",
                app.getLendingService().flagOverdueLoans() == 1);

        Loan closed = app.getLendingService().returnCopy(loan.getBarcode());
        check("A fine is charged on return", closed.getFineCharged() > 0);
    }

    private void testReservationQueue() {
        section("Reservations");
        LibraryApplication app = seededApp();
        check("Reserving an on-shelf title is refused", throwsType(ReservationException.class,
                () -> app.getReservationService().reserve("P-002", "9780132350884", "BR-DEL")));

        Loan loan = app.getLendingService().checkout("P-001", "9780134685991", "BR-DEL");
        Reservation first = app.getReservationService().reserve("P-002", "9780134685991", "BR-DEL");
        check("A reservation is placed when nothing is free", first.isWaiting());
        check("The first reservation is at the head of the queue",
                app.getReservationService().getQueuePosition("9780134685991", "P-002") == 1);

        app.getReservationService().reserve("P-003", "9780134685991", "BR-DEL");
        check("The second reservation queues behind the first",
                app.getReservationService().getQueuePosition("9780134685991", "P-003") == 2);
        check("The queue holds both", app.getReservationService().getQueue("9780134685991").size() == 2);
        check("Reserving twice is refused", throwsType(ReservationException.class,
                () -> app.getReservationService().reserve("P-002", "9780134685991", "BR-DEL")));

        app.getReservationService().cancel(first.getReservationId());
        check("Cancelling closes the reservation",
                first.getStatus() == ReservationStatus.CANCELLED);
        check("Cancelling promotes the next in line",
                app.getReservationService().getQueuePosition("9780134685991", "P-003") == 1);
        check("Cancelling twice is refused", throwsType(ReservationException.class,
                () -> app.getReservationService().cancel(first.getReservationId())));
    }

    private void testReservationNotification() {
        section("Reservation fulfilment and notification");
        LibraryApplication app = seededApp();
        Loan loan = app.getLendingService().checkout("P-001", "9780134685991", "BR-DEL");
        Reservation reservation =
                app.getReservationService().reserve("P-002", "9780134685991", "BR-DEL");
        int noticesBefore = app.getNotificationService().getSentCount();

        app.getLendingService().returnCopy(loan.getBarcode());

        check("The reservation becomes ready on return",
                reservation.getStatus() == ReservationStatus.READY);
        check("A copy is held against the reservation", reservation.getHeldBarcode() != null);
        check("The held copy is not shelved", app.getInventoryService()
                .findByBarcode(reservation.getHeldBarcode()).getStatus() == CopyStatus.ON_HOLD);
        check("Nobody can borrow the held copy",
                throwsType(CopyNotAvailableException.class,
                        () -> app.getLendingService().checkout("P-003", "9780134685991", null)));
        check("The waiting patron is notified",
                app.getNotificationService().getSentCount() > noticesBefore);
        check("A ready event was published", app.getEventPublisher().getHistory().stream()
                .anyMatch(event -> event.getType()
                        == com.library.domain.enums.EventType.RESERVATION_READY));
        check("The queue is now empty",
                app.getReservationService().getQueue("9780134685991").isEmpty());
    }

    private void testMultiBranchTransfer() {
        section("Multi-branch transfers");
        LibraryApplication app = seededApp();
        ItemCopy copy = app.getInventoryService().listByItem("9780132350884").stream()
                .filter(candidate -> candidate.getBranchId().equals("BR-DEL"))
                .findFirst().orElseThrow();
        String origin = copy.getBranchId();

        app.getTransferService().initiateTransfer(copy.getBarcode(), "BR-MUM");
        check("A dispatched copy is in transit", copy.getStatus() == CopyStatus.IN_TRANSIT);
        check("It still counts at its origin until received", origin.equals(copy.getBranchId()));
        check("A copy in transit cannot be lent",
                throwsType(CopyNotAvailableException.class,
                        () -> app.getLendingService().checkoutCopy("P-001", copy.getBarcode())));
        check("In-transit copies are listed",
                app.getTransferService().listInTransit().size() == 1);
        check("Transferring an in-transit copy again is refused",
                throwsType(TransferException.class,
                        () -> app.getTransferService().initiateTransfer(copy.getBarcode(), "BR-BLR")));

        app.getTransferService().completeTransfer(copy.getBarcode());
        check("Receiving moves the copy", "BR-MUM".equals(copy.getBranchId()));
        check("A received copy is lendable again", copy.getStatus() == CopyStatus.AVAILABLE);
        check("Nothing is left in transit", app.getTransferService().listInTransit().isEmpty());
        check("Transferring to the branch it is already at is refused",
                throwsType(TransferException.class,
                        () -> app.getTransferService().initiateTransfer(copy.getBarcode(), "BR-MUM")));
    }

    private void testRecommendations() {
        section("Recommendations");
        LibraryApplication app = seededApp();
        borrowAndReturn(app, "P-001", "9780201633610");
        borrowAndReturn(app, "P-002", "9780201633610");
        borrowAndReturn(app, "P-002", "9780062316110");

        List<LibraryItem> blended = app.getRecommendationService().recommendFor("P-001", 3);
        check("The blended strategy produces suggestions", !blended.isEmpty());
        check("It never suggests what the patron already read",
                blended.stream().noneMatch(item -> item.getId().equals("9780201633610")));
        check("The limit is respected", blended.size() <= 3);

        for (RecommendationStrategy strategy : app.getRecommendationService().getAvailableStrategies()) {
            check("Strategy '" + strategy.getStrategyName() + "' returns without error",
                    app.getRecommendationService().recommendFor("P-001", strategy, 3) != null);
        }

        List<LibraryItem> collaborative = app.getRecommendationService().recommendFor("P-001",
                new com.library.recommendation.CollaborativeFilteringStrategy(
                        new com.library.repository.inmemory.InMemoryPatronRepository()), 3);
        check("Collaborative filtering copes with an empty neighbour set",
                collaborative.isEmpty());

        Patron newcomer = app.getPatronService().registerPatron("New Member", "new@example.com");
        check("A patron with no history still gets popularity suggestions",
                !app.getRecommendationService().recommendFor(newcomer.getPatronId(),
                        new com.library.recommendation.PopularityStrategy(
                                new com.library.repository.inmemory.InMemoryLoanRepository()), 3)
                        .equals(null));
    }

    private void testObserverBus() {
        section("Observer pattern");
        LibraryApplication app = seededApp();
        int listeners = app.getEventPublisher().getListeners().size();
        check("Listeners are subscribed at startup", listeners >= 2);

        int before = app.getEventPublisher().getHistory().size();
        Loan loan = app.getLendingService().checkout("P-001", "9780132350884", null);
        check("Borrowing publishes an event",
                app.getEventPublisher().getHistory().size() > before);

        LibraryEvent latest = app.getEventPublisher().getHistory()
                .get(app.getEventPublisher().getHistory().size() - 1);
        check("The event names the borrower", "P-001".equals(latest.getActorId()));
        check("The event carries a payload", latest.getPayload().containsKey("barcode"));
        check("The payload is read only", throwsType(UnsupportedOperationException.class,
                () -> latest.getPayload().put("tampered", "yes")));
        check("The audit trail recorded it",
                !app.getAuditTrailListener().getEntries().isEmpty());

        app.getLendingService().returnCopy(loan.getBarcode());
        check("Returning publishes an event too", app.getEventPublisher().getHistory().stream()
                .anyMatch(event -> event.getType()
                        == com.library.domain.enums.EventType.BOOK_RETURNED));
    }

    private void testPolymorphismAndLsp() {
        section("Polymorphism and substitutability");
        LibraryApplication app = seededApp();
        app.getCatalogService().addItem(new Magazine("ISSN-1234-5678", "Nature Weekly",
                2024, Genre.SCIENCE, "Springer", 42));
        app.getInventoryService().addCopy("ISSN-1234-5678", "BR-DEL");

        List<LibraryItem> catalogue = app.getCatalogService().listAll();
        check("Books and magazines share one catalogue",
                catalogue.stream().anyMatch(item -> item instanceof Book)
                        && catalogue.stream().anyMatch(item -> item instanceof Magazine));

        for (LibraryItem item : catalogue) {
            if (item.getSummary() == null || item.getItemType() == null) {
                check("Every item answers the abstract contract", false);
                return;
            }
        }
        check("Every item answers the abstract contract", true);

        check("Search works across formats without a cast",
                app.getCatalogService().search(new TitleSearchStrategy(), "Nature").size() == 1);
        check("Author search reads the contributor polymorphically",
                app.getCatalogService().search(new AuthorSearchStrategy(), "Springer").size() == 1);

        Loan magazineLoan = app.getLendingService().checkout("P-001", "ISSN-1234-5678", "BR-DEL");
        check("A magazine lends through the same code path as a book", magazineLoan.isActive());
        app.getLendingService().returnCopy(magazineLoan.getBarcode());
        check("A magazine returns through the same code path", !magazineLoan.isActive());

        LibraryItem asBase = new Book.Builder("9780140449136", "The Odyssey").author("Homer").build();
        check("A subclass is usable through its base type",
                "Homer".equals(asBase.getContributor()));
        check("Equality is by catalogue key",
                asBase.equals(new Book.Builder("9780140449136", "Different Title").build()));
        check("A book never equals a magazine with the same key",
                !asBase.equals(new Magazine("9780140449136", "The Odyssey", 2020,
                        Genre.POETRY, "Penguin", 1)));
    }

    private void borrowAndReturn(LibraryApplication app, String patronId, String itemId) {
        try {
            Loan loan = app.getLendingService().checkout(patronId, itemId, null);
            app.getLendingService().returnCopy(loan.getBarcode());
        } catch (LibraryException e) {
            failures.add("Setup failed: " + e.getMessage());
        }
    }

    private LibraryApplication freshApp() {
        com.library.util.IdGenerator.reset();
        return new LibraryApplication();
    }

    private LibraryApplication seededApp() {
        LibraryApplication app = freshApp();
        app.getTransferService().addBranch("BR-DEL", "Central Library", "Delhi");
        app.getTransferService().addBranch("BR-MUM", "Marine Drive Branch", "Mumbai");
        app.getTransferService().addBranch("BR-BLR", "Koramangala Branch", "Bengaluru");

        app.getCatalogService().addBook("9780132350884", "Clean Code",
                "Robert C. Martin", 2008, Genre.TECHNOLOGY);
        app.getCatalogService().addBook("9780134685991", "Effective Java",
                "Joshua Bloch", 2018, Genre.TECHNOLOGY);
        app.getCatalogService().addBook("9780201633610", "Design Patterns",
                "Erich Gamma", 1994, Genre.TECHNOLOGY);
        app.getCatalogService().addBook("9780345391803", "Hitchhiker's Guide",
                "Douglas Adams", 1979, Genre.FANTASY);
        app.getCatalogService().addBook("9780062316110", "Sapiens",
                "Yuval Noah Harari", 2011, Genre.HISTORY);
        app.getCatalogService().addBook("9781400052929", "Freakonomics",
                "Steven Levitt", 2005, Genre.NON_FICTION);

        app.getInventoryService().addCopies("9780132350884", "BR-DEL", 2);
        app.getInventoryService().addCopy("9780134685991", "BR-DEL");
        app.getInventoryService().addCopy("9780201633610", "BR-DEL");
        app.getInventoryService().addCopy("9780345391803", "BR-MUM");
        app.getInventoryService().addCopy("9780062316110", "BR-DEL");
        app.getInventoryService().addCopy("9781400052929", "BR-BLR");

        app.getPatronService().registerPatron("Aarav Sharma", "aarav@example.com", "9900011111",
                MembershipType.BASIC, NotificationChannelType.CONSOLE);
        app.getPatronService().registerPatron("Diya Nair", "diya@example.com", "9900011112",
                MembershipType.STUDENT, NotificationChannelType.CONSOLE);
        app.getPatronService().registerPatron("Rohan Verma", "rohan@example.com", "9900011113",
                MembershipType.PREMIUM, NotificationChannelType.CONSOLE);
        return app;
    }

    private boolean throwsInvalid(Runnable action) {
        return throwsType(InvalidInputException.class, action);
    }

    private boolean throwsType(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            return false;
        } catch (RuntimeException e) {
            return expected.isInstance(e);
        }
    }

    private Throwable causeOf(Runnable action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException e) {
            return e.getCause();
        }
    }

    private void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.printf("  [PASS] %s%n", description);
        } else {
            failed++;
            failures.add(description);
            System.out.printf("  [FAIL] %s%n", description);
        }
    }

    private void section(String title) {
        System.out.println();
        System.out.println("  " + title);
        System.out.println("  " + "-".repeat(76));
    }

    private void summary() {
        System.out.println();
        System.out.println(LINE);
        System.out.printf("  RESULT: %d passed, %d failed, %d total%n", passed, failed, passed + failed);
        if (!failures.isEmpty()) {
            System.out.println("  Failing checks:");
            failures.forEach(failure -> System.out.println("    - " + failure));
        }
        System.out.println(LINE);
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }
}
