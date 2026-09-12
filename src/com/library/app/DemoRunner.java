package com.library.app;

import com.library.domain.enums.Genre;
import com.library.domain.enums.MembershipType;
import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.ItemCopy;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Loan;
import com.library.domain.model.Patron;
import com.library.domain.model.Reservation;
import com.library.exception.CopyNotAvailableException;
import com.library.exception.LibraryException;
import com.library.recommendation.RecommendationStrategy;
import com.library.search.AuthorSearchStrategy;
import com.library.search.CompositeSearchStrategy;
import com.library.search.IsbnSearchStrategy;
import com.library.search.TitleSearchStrategy;
import com.library.util.Validator;

import java.util.List;
import java.util.Map;

/**
 * A scripted walk through every feature, so a reviewer can see the whole system
 * behave without driving the menus by hand.
 */
public class DemoRunner {

    private static final String LINE =
            "================================================================================";
    private static final String THIN =
            "--------------------------------------------------------------------------------";

    private final LibraryApplication app;

    public DemoRunner(LibraryApplication app) {
        this.app = app;
    }

    /**
     * Runs the scripted scenario end to end.
     */
    public void run() {
        header("LIBRARY MANAGEMENT SYSTEM - GUIDED DEMONSTRATION");

        SampleData.load(app);

        section("1. CATALOGUE");
        app.getCatalogService().listAll().forEach(item -> print(item.getSummary()));
        print("Catalogue holds " + app.getCatalogService().getCatalogueSize() + " records.");

        section("2. SEARCH - the same catalogue, three strategies");
        runSearch("Title contains 'clean'", new TitleSearchStrategy(), "clean");
        runSearch("Author contains 'martin'", new AuthorSearchStrategy(), "martin");
        runSearch("ISBN 9780132350884 (hyphens ignored)", new IsbnSearchStrategy(), "978-0132350884");
        runSearch("Any field: 'java'", new CompositeSearchStrategy(
                new TitleSearchStrategy(), new AuthorSearchStrategy(), new IsbnSearchStrategy()), "java");

        section("3. INVENTORY across branches");
        app.getTransferService().listBranches().forEach(branch -> print(branch.toString()));
        print("");
        app.getInventoryService().countByBranch()
                .forEach((branch, count) -> print(String.format("  %-8s holds %d copies", branch, count)));

        section("4. LENDING - policy varies by membership tier");
        Patron aarav = app.getPatronService().findById("P-001");
        Patron diya = app.getPatronService().findById("P-002");
        print(String.format("%s is %s: %d day loan, %d book limit",
                aarav.getName(), aarav.getMembershipType().getLabel(),
                app.getPolicyResolver().resolve(aarav.getMembershipType()).getLoanPeriodDays(),
                app.getPolicyResolver().resolve(aarav.getMembershipType()).getMaxConcurrentLoans()));
        print(String.format("%s is %s: %d day loan, %d book limit",
                diya.getName(), diya.getMembershipType().getLabel(),
                app.getPolicyResolver().resolve(diya.getMembershipType()).getLoanPeriodDays(),
                app.getPolicyResolver().resolve(diya.getMembershipType()).getMaxConcurrentLoans()));
        print("");

        Loan loan1 = app.getLendingService().checkout("P-001", "9780132350884", "BR-DEL");
        print("Loan created: " + loan1);

        section("5. AVAILABILITY tracking");
        showAvailability("9780132350884");

        section("6. RESERVATION - every copy is out, so the queue forms");
        String scarce = "9780134685991";
        print("Lending the only copy of 'Effective Java'...");
        app.getLendingService().checkout("P-001", scarce, "BR-DEL");
        showAvailability(scarce);
        print("");
        try {
            app.getLendingService().checkout("P-002", scarce, "BR-DEL");
        } catch (CopyNotAvailableException e) {
            print("Checkout refused: " + e.getMessage());
        }
        print("");
        print("So P-002 reserves it instead:");
        Reservation reservation = app.getReservationService().reserve("P-002", scarce, "BR-DEL");
        print("Queue position for P-002: "
                + app.getReservationService().getQueuePosition(scarce, "P-002"));
        print("");
        print("P-003 joins the queue behind them:");
        app.getReservationService().reserve("P-003", scarce, "BR-DEL");
        print("Queue position for P-003: "
                + app.getReservationService().getQueuePosition(scarce, "P-003"));

        section("7. RETURN triggers the notification, through the event bus");
        Loan effectiveJavaLoan = app.getLendingService().getActiveLoans("P-001").stream()
                .filter(loan -> loan.getItemId().equals(scarce))
                .findFirst().orElseThrow();
        print("P-001 returns copy " + effectiveJavaLoan.getBarcode() + "...");
        print("");
        app.getLendingService().returnCopy(effectiveJavaLoan.getBarcode());
        print("");
        print("Reservation " + reservation.getReservationId() + " is now "
                + app.getReservationService().getReservationsFor("P-002").get(0).getStatus().getLabel());
        print("The copy is held, not shelved, so nobody can borrow it ahead of P-002:");
        showAvailability(scarce);

        section("8. MULTI-BRANCH TRANSFER");
        ItemCopy toMove = app.getInventoryService().listByItem("9780132350884").stream()
                .filter(ItemCopy::isAvailable)
                .findFirst().orElseThrow();
        print("Moving copy " + toMove.getBarcode() + " from " + toMove.getBranchId() + " to BR-MUM");
        app.getTransferService().initiateTransfer(toMove.getBarcode(), "BR-MUM");
        print("In transit: " + app.getTransferService().listInTransit().size() + " copy");
        print("A copy in transit cannot be lent:");
        try {
            app.getLendingService().checkoutCopy("P-003", toMove.getBarcode());
        } catch (LibraryException e) {
            print("  refused - " + e.getMessage());
        }
        app.getTransferService().completeTransfer(toMove.getBarcode());
        print("Received at " + app.getInventoryService()
                .findByBarcode(toMove.getBarcode()).getBranchId() + " and lendable again.");

        section("9. RECOMMENDATIONS - four signals, blended");
        for (RecommendationStrategy strategy
                : app.getRecommendationService().getAvailableStrategies()) {
            List<LibraryItem> suggestions =
                    app.getRecommendationService().recommendFor("P-001", strategy, 3);
            print(String.format("%-24s -> %s", strategy.getStrategyName(),
                    suggestions.isEmpty() ? "(nothing to suggest)" : titlesOf(suggestions)));
        }

        section("10. BORROWING HISTORY drives those suggestions");
        for (String patronId : List.of("P-001", "P-002", "P-003")) {
            Patron patron = app.getPatronService().findById(patronId);
            print(String.format("%-8s %-18s borrowed %s", patronId, patron.getName(),
                    patron.getBorrowingHistory()));
        }

        section("11. AUDIT TRAIL - every event the observers saw");
        List<String> entries = app.getAuditTrailListener().getEntries();
        entries.stream().skip(Math.max(0, entries.size() - 8))
                .forEach(entry -> print(entry.substring(entry.indexOf('|') + 2)));
        print("");
        print("Total events published : " + app.getEventPublisher().getHistory().size());
        print("Total notices delivered: " + app.getNotificationService().getSentCount());

        header("DEMONSTRATION COMPLETE");
    }

    private void runSearch(String label, com.library.search.SearchStrategy strategy, String query) {
        List<LibraryItem> results = app.getCatalogService().search(strategy, query);
        print(String.format("%-42s -> %d result(s): %s", label, results.size(), titlesOf(results)));
    }

    private String titlesOf(List<LibraryItem> items) {
        if (items.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append('"').append(items.get(i).getTitle()).append('"');
        }
        return builder.toString();
    }

    private void showAvailability(String itemId) {
        LibraryItem item = app.getCatalogService().findById(itemId);
        print(String.format("'%s': %d of %d copies available, %d on loan",
                item.getTitle(),
                app.getInventoryService().getAvailableCount(itemId),
                app.getInventoryService().getTotalCount(itemId),
                app.getInventoryService().getBorrowedCount(itemId)));
    }

    private void header(String title) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  " + title);
        System.out.println(LINE);
    }

    private void section(String title) {
        System.out.println();
        System.out.println(THIN);
        System.out.println("  " + title);
        System.out.println(THIN);
    }

    private void print(String text) {
        System.out.println("  " + text);
    }

    /**
     * Loads a small, realistic dataset. Kept separate from the demo script so the
     * interactive console can use the same data.
     */
    public static final class SampleData {

        private SampleData() {
        }

        /**
         * @param app the wired application to populate
         */
        public static void load(LibraryApplication app) {
            app.getTransferService().addBranch("BR-DEL", "Central Library", "Delhi");
            app.getTransferService().addBranch("BR-MUM", "Marine Drive Branch", "Mumbai");
            app.getTransferService().addBranch("BR-BLR", "Koramangala Branch", "Bengaluru");

            app.getCatalogService().addBook("9780132350884", "Clean Code",
                    "Robert C. Martin", 2008, Genre.TECHNOLOGY);
            app.getCatalogService().addBook("9780134685991", "Effective Java",
                    "Joshua Bloch", 2018, Genre.TECHNOLOGY);
            app.getCatalogService().addBook("9780201633610", "Design Patterns",
                    "Erich Gamma", 1994, Genre.TECHNOLOGY);
            app.getCatalogService().addBook("9780134494166", "Clean Architecture",
                    "Robert C. Martin", 2017, Genre.TECHNOLOGY);
            app.getCatalogService().addBook("9780596009205", "Head First Design Patterns",
                    "Eric Freeman", 2004, Genre.TECHNOLOGY);
            app.getCatalogService().addBook("9780345391803", "The Hitchhiker's Guide to the Galaxy",
                    "Douglas Adams", 1979, Genre.FANTASY);
            app.getCatalogService().addBook("9780307474278", "The Da Vinci Code",
                    "Dan Brown", 2003, Genre.MYSTERY);
            app.getCatalogService().addBook("9780062316110", "Sapiens",
                    "Yuval Noah Harari", 2011, Genre.HISTORY);
            app.getCatalogService().addBook("9781400052929", "Freakonomics",
                    "Steven Levitt", 2005, Genre.NON_FICTION);
            app.getCatalogService().addBook("9780593139134", "Project Hail Mary",
                    "Andy Weir", 2021, Genre.SCIENCE);

            app.getCatalogService().addItem(new com.library.domain.model.Magazine(
                    "ISSN-2049-3630", "Java Magazine", 2024, Genre.TECHNOLOGY, "Oracle", 112));

            app.getInventoryService().addCopies("9780132350884", "BR-DEL", 2);
            app.getInventoryService().addCopy("9780132350884", "BR-MUM");
            app.getInventoryService().addCopy("9780134685991", "BR-DEL");
            app.getInventoryService().addCopies("9780201633610", "BR-DEL", 2);
            app.getInventoryService().addCopy("9780134494166", "BR-MUM");
            app.getInventoryService().addCopy("9780596009205", "BR-BLR");
            app.getInventoryService().addCopies("9780345391803", "BR-DEL", 2);
            app.getInventoryService().addCopy("9780307474278", "BR-MUM");
            app.getInventoryService().addCopy("9780062316110", "BR-DEL");
            app.getInventoryService().addCopy("9781400052929", "BR-BLR");
            app.getInventoryService().addCopy("9780593139134", "BR-DEL");
            app.getInventoryService().addCopy("ISSN-2049-3630", "BR-DEL");

            Patron aarav = app.getPatronService().registerPatron("Aarav Sharma",
                    "aarav.sharma@example.com", "9900011111",
                    MembershipType.BASIC, NotificationChannelType.EMAIL);
            Patron diya = app.getPatronService().registerPatron("Diya Nair",
                    "diya.nair@example.com", "9900011112",
                    MembershipType.STUDENT, NotificationChannelType.SMS);
            Patron rohan = app.getPatronService().registerPatron("Rohan Verma",
                    "rohan.verma@example.com", "9900011113",
                    MembershipType.PREMIUM, NotificationChannelType.CONSOLE);

            aarav.addPreferredGenre(Genre.TECHNOLOGY);
            diya.addPreferredGenre(Genre.SCIENCE);
            rohan.addPreferredGenre(Genre.HISTORY);

            seedHistory(app, "P-001", "9780201633610");
            seedHistory(app, "P-002", "9780201633610");
            seedHistory(app, "P-002", "9780062316110");
            seedHistory(app, "P-003", "9780062316110");
            seedHistory(app, "P-003", "9781400052929");
        }

        /**
         * Borrows and immediately returns a title, so the patron gains a history
         * entry and the loan ledger gains a record without leaving a copy out.
         */
        private static void seedHistory(LibraryApplication app, String patronId, String itemId) {
            try {
                Loan loan = app.getLendingService().checkout(patronId, itemId, null);
                app.getLendingService().returnCopy(loan.getBarcode());
            } catch (LibraryException e) {
                System.out.println("  (sample data skipped: " + e.getMessage() + ")");
            }
        }

        /**
         * @param attributes catalogue fields, used by the console's add-item flow
         * @return the fields validated and trimmed
         */
        public static Map<String, String> validated(Map<String, String> attributes) {
            attributes.forEach((key, value) -> {
                if (Validator.isBlank(value)) {
                    attributes.put(key, "");
                }
            });
            return attributes;
        }
    }
}
