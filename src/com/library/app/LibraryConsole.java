package com.library.app;

import com.library.domain.enums.Genre;
import com.library.domain.enums.MembershipType;
import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Branch;
import com.library.domain.model.ItemCopy;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Loan;
import com.library.domain.model.Patron;
import com.library.domain.model.Reservation;
import com.library.exception.LibraryException;
import com.library.recommendation.RecommendationStrategy;
import com.library.search.AuthorSearchStrategy;
import com.library.search.CompositeSearchStrategy;
import com.library.search.GenreSearchStrategy;
import com.library.search.IsbnSearchStrategy;
import com.library.search.SearchStrategy;
import com.library.search.TitleSearchStrategy;
import com.library.util.Validator;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * The interactive console.
 *
 * <p>Deliberately thin: it prints menus, reads input and calls services. It
 * holds no business rules, which is why the same services can be driven by the
 * demo runner and the test suite with no console involved at all.</p>
 */
public class LibraryConsole {

    private static final String LINE =
            "================================================================================";
    private static final String THIN =
            "--------------------------------------------------------------------------------";

    private final Scanner scanner = new Scanner(System.in);
    private final LibraryApplication app;

    public LibraryConsole(LibraryApplication app) {
        this.app = app;
    }

    /**
     * Runs the menu loop until the operator exits.
     */
    public void start() {
        banner();
        boolean running = true;
        while (running) {
            mainMenu();
            switch (readChoice()) {
                case 1: catalogueMenu(); break;
                case 2: patronMenu(); break;
                case 3: lendingMenu(); break;
                case 4: reservationMenu(); break;
                case 5: inventoryMenu(); break;
                case 6: recommendationMenu(); break;
                case 7: auditMenu(); break;
                case 0: running = false; goodbye(); break;
                default: error("That option is not on this menu.");
            }
        }
    }

    private void banner() {
        System.out.println(LINE);
        System.out.println("  LIBRARY MANAGEMENT SYSTEM");
        System.out.println(LINE);
        info(String.format("Loaded %d catalogue records, %d patrons, %d branches.",
                app.getCatalogService().getCatalogueSize(),
                app.getPatronService().getPatronCount(),
                app.getTransferService().listBranches().size()));
    }

    private void mainMenu() {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  MAIN MENU");
        System.out.println(LINE);
        System.out.println("  1. Catalogue (add, update, remove, search)");
        System.out.println("  2. Patrons (register, update, history)");
        System.out.println("  3. Lending (checkout, return, renew, overdue)");
        System.out.println("  4. Reservations");
        System.out.println("  5. Inventory and branches");
        System.out.println("  6. Recommendations");
        System.out.println("  7. Events and audit trail");
        System.out.println("  0. Exit");
        System.out.println(THIN);
        System.out.print("  Choice: ");
    }

    private void catalogueMenu() {
        boolean open = true;
        while (open) {
            section("CATALOGUE");
            System.out.println("  1. List everything");
            System.out.println("  2. Add a book");
            System.out.println("  3. Update a record");
            System.out.println("  4. Remove a record");
            System.out.println("  5. Search");
            System.out.println("  0. Back");
            System.out.print("  Choice: ");
            try {
                switch (readChoice()) {
                    case 1:
                        printItems(app.getCatalogService().listAll());
                        break;
                    case 2:
                        addBook();
                        break;
                    case 3:
                        updateItem();
                        break;
                    case 4:
                        app.getCatalogService().removeItem(read("ISBN: "));
                        ok("Record withdrawn.");
                        break;
                    case 5:
                        search();
                        break;
                    case 0:
                        open = false;
                        break;
                    default:
                        error("That option is not on this menu.");
                }
            } catch (LibraryException e) {
                error(e.getMessage());
            }
        }
    }

    private void addBook() {
        String isbn = read("ISBN (10 or 13 digits): ");
        String title = read("Title: ");
        String author = read("Author: ");
        int year = readInt("Publication year: ");
        Genre genre = readGenre();
        LibraryItem item = app.getCatalogService().addBook(isbn, title, author, year, genre);
        ok("Catalogued: " + item.getTitle());
        String branch = read("Add a copy now? Branch ID (Enter to skip): ");
        if (!Validator.isBlank(branch)) {
            ItemCopy copy = app.getInventoryService().addCopy(isbn, branch.trim());
            ok("Copy " + copy.getBarcode() + " added at " + copy.getBranchId() + ".");
        }
    }

    private void updateItem() {
        String isbn = read("ISBN: ");
        LibraryItem existing = app.getCatalogService().findById(isbn);
        info("Current: " + existing.getSummary());
        info("Press Enter to keep a value.");
        String title = read("New title: ");
        String yearInput = read("New year: ");
        int year = Validator.isBlank(yearInput) ? 0 : Validator.parseInt(yearInput, "year");
        String genreInput = read("New genre (Enter to skip): ");
        Genre genre = Validator.isBlank(genreInput) ? null : Genre.fromText(genreInput);
        app.getCatalogService().updateItem(isbn, title, year, genre);
        ok("Record updated.");
    }

    private void search() {
        System.out.println("  1. Title   2. Author   3. ISBN   4. Genre   5. Any field");
        int pick = readInt("Search by: ");
        SearchStrategy strategy;
        switch (pick) {
            case 2: strategy = new AuthorSearchStrategy(); break;
            case 3: strategy = new IsbnSearchStrategy(); break;
            case 4: strategy = new GenreSearchStrategy(); break;
            case 5: strategy = new CompositeSearchStrategy(new TitleSearchStrategy(),
                    new AuthorSearchStrategy(), new IsbnSearchStrategy(), new GenreSearchStrategy());
                break;
            default: strategy = new TitleSearchStrategy();
        }
        List<LibraryItem> results = app.getCatalogService().search(strategy, read("Query: "));
        info("Strategy used: " + strategy.getName());
        printItems(results);
    }

    private void patronMenu() {
        boolean open = true;
        while (open) {
            section("PATRONS");
            System.out.println("  1. List patrons");
            System.out.println("  2. Register a patron");
            System.out.println("  3. Update contact details");
            System.out.println("  4. Borrowing history");
            System.out.println("  5. Block or unblock");
            System.out.println("  0. Back");
            System.out.print("  Choice: ");
            try {
                switch (readChoice()) {
                    case 1:
                        app.getPatronService().listAll().forEach(p -> System.out.println("  " + p));
                        break;
                    case 2:
                        registerPatron();
                        break;
                    case 3:
                        String targetId = read("Patron ID: ");
                        String newName = read("New name: ");
                        String newEmail = read("New email: ");
                        app.getPatronService().updatePatron(targetId, newName, newEmail,
                                read("New phone: "));
                        ok("Patron updated.");
                        break;
                    case 4:
                        showHistory();
                        break;
                    case 5:
                        app.getPatronService().setActive(read("Patron ID: "),
                                read("Active? (y/n): ").trim().toLowerCase().startsWith("y"));
                        ok("Membership updated.");
                        break;
                    case 0:
                        open = false;
                        break;
                    default:
                        error("That option is not on this menu.");
                }
            } catch (LibraryException e) {
                error(e.getMessage());
            }
        }
    }

    private void registerPatron() {
        String name = read("Name: ");
        String email = read("Email: ");
        String phone = read("Phone: ");
        System.out.println("  1. Basic   2. Student   3. Premium");
        int tier = readInt("Membership: ");
        MembershipType type = tier == 2 ? MembershipType.STUDENT
                : tier == 3 ? MembershipType.PREMIUM : MembershipType.BASIC;
        System.out.println("  1. Console   2. Email   3. SMS");
        int channelPick = readInt("Preferred contact: ");
        NotificationChannelType channel = channelPick == 2 ? NotificationChannelType.EMAIL
                : channelPick == 3 ? NotificationChannelType.SMS : NotificationChannelType.CONSOLE;
        Patron patron = app.getPatronService().registerPatron(name, email, phone, type, channel);
        ok("Registered " + patron.getPatronId() + " on " + type.getLabel() + " terms.");
    }

    private void showHistory() {
        String patronId = read("Patron ID: ");
        List<String> history = app.getPatronService().getBorrowingHistory(patronId);
        if (history.isEmpty()) {
            info("Nothing borrowed yet.");
            return;
        }
        for (String itemId : history) {
            try {
                System.out.println("  " + app.getCatalogService().findById(itemId).getSummary());
            } catch (LibraryException e) {
                System.out.println("  " + itemId + " (withdrawn from catalogue)");
            }
        }
    }

    private void lendingMenu() {
        boolean open = true;
        while (open) {
            section("LENDING");
            System.out.println("  1. Check out");
            System.out.println("  2. Return");
            System.out.println("  3. Renew");
            System.out.println("  4. Active loans for a patron");
            System.out.println("  5. Overdue loans");
            System.out.println("  6. Flag overdue loans and notify");
            System.out.println("  0. Back");
            System.out.print("  Choice: ");
            try {
                switch (readChoice()) {
                    case 1: {
                        String patronId = read("Patron ID: ");
                        String isbn = read("ISBN: ");
                        String branch = read("Preferred branch (Enter for any): ");
                        Loan loan = app.getLendingService().checkout(patronId, isbn,
                                Validator.isBlank(branch) ? null : branch.trim());
                        ok("Loan " + loan.getLoanId() + " created, due " + loan.getDueOn() + ".");
                        break;
                    }
                    case 2: {
                        Loan loan = app.getLendingService().returnCopy(read("Copy barcode: "));
                        ok("Returned. Fine charged: " + String.format("%.2f", loan.getFineCharged()));
                        break;
                    }
                    case 3: {
                        Loan loan = app.getLendingService().renew(read("Loan ID: "));
                        ok("Renewed to " + loan.getDueOn() + ".");
                        break;
                    }
                    case 4:
                        printLoans(app.getLendingService().getActiveLoans(read("Patron ID: ")));
                        break;
                    case 5:
                        printLoans(app.getLendingService().findOverdueLoans());
                        break;
                    case 6:
                        ok(app.getLendingService().flagOverdueLoans() + " loan(s) flagged.");
                        break;
                    case 0:
                        open = false;
                        break;
                    default:
                        error("That option is not on this menu.");
                }
            } catch (LibraryException e) {
                error(e.getMessage());
            }
        }
    }

    private void reservationMenu() {
        boolean open = true;
        while (open) {
            section("RESERVATIONS");
            System.out.println("  1. Reserve a title");
            System.out.println("  2. Queue for a title");
            System.out.println("  3. Reservations for a patron");
            System.out.println("  4. Cancel a reservation");
            System.out.println("  0. Back");
            System.out.print("  Choice: ");
            try {
                switch (readChoice()) {
                    case 1: {
                        String patronId = read("Patron ID: ");
                        String isbn = read("ISBN: ");
                        String branch = read("Collect at branch: ");
                        Reservation reservation =
                                app.getReservationService().reserve(patronId, isbn, branch);
                        ok("Reservation " + reservation.getReservationId() + " placed.");
                        break;
                    }
                    case 2: {
                        List<Reservation> queue = app.getReservationService().getQueue(read("ISBN: "));
                        if (queue.isEmpty()) {
                            info("Nobody is waiting for that title.");
                        }
                        for (int i = 0; i < queue.size(); i++) {
                            System.out.println("  " + (i + 1) + ". " + queue.get(i));
                        }
                        break;
                    }
                    case 3: {
                        List<Reservation> mine =
                                app.getReservationService().getReservationsFor(read("Patron ID: "));
                        if (mine.isEmpty()) {
                            info("No reservations on file.");
                        }
                        mine.forEach(r -> System.out.println("  " + r));
                        break;
                    }
                    case 4:
                        app.getReservationService().cancel(read("Reservation ID: "));
                        ok("Reservation cancelled.");
                        break;
                    case 0:
                        open = false;
                        break;
                    default:
                        error("That option is not on this menu.");
                }
            } catch (LibraryException e) {
                error(e.getMessage());
            }
        }
    }

    private void inventoryMenu() {
        boolean open = true;
        while (open) {
            section("INVENTORY AND BRANCHES");
            System.out.println("  1. Availability of a title");
            System.out.println("  2. Copies of a title");
            System.out.println("  3. Copies at a branch");
            System.out.println("  4. Add copies");
            System.out.println("  5. List branches");
            System.out.println("  6. Transfer a copy to another branch");
            System.out.println("  7. Receive a copy in transit");
            System.out.println("  0. Back");
            System.out.print("  Choice: ");
            try {
                switch (readChoice()) {
                    case 1: {
                        String isbn = read("ISBN: ");
                        LibraryItem item = app.getCatalogService().findById(isbn);
                        info(String.format("'%s': %d of %d available, %d on loan",
                                item.getTitle(),
                                app.getInventoryService().getAvailableCount(isbn),
                                app.getInventoryService().getTotalCount(isbn),
                                app.getInventoryService().getBorrowedCount(isbn)));
                        break;
                    }
                    case 2:
                        app.getInventoryService().listByItem(read("ISBN: "))
                                .forEach(copy -> System.out.println("  " + copy));
                        break;
                    case 3:
                        app.getInventoryService().listByBranch(read("Branch ID: "))
                                .forEach(copy -> System.out.println("  " + copy));
                        break;
                    case 4: {
                        String isbn = read("ISBN: ");
                        String branchId = read("Branch ID: ");
                        List<ItemCopy> created =
                                app.getInventoryService().addCopies(isbn, branchId, readInt("How many: "));
                        ok(created.size() + " copy/copies added.");
                        break;
                    }
                    case 5:
                        for (Branch branch : app.getTransferService().listBranches()) {
                            System.out.printf("  %s | %d copies held%n", branch,
                                    app.getInventoryService().listByBranch(branch.getBranchId()).size());
                        }
                        break;
                    case 6:
                        String movingBarcode = read("Copy barcode: ");
                        app.getTransferService().initiateTransfer(movingBarcode,
                                read("Destination branch: "));
                        ok("Copy dispatched and marked in transit.");
                        break;
                    case 7: {
                        ItemCopy copy = app.getTransferService().completeTransfer(read("Copy barcode: "));
                        ok("Received at " + copy.getBranchId() + " and available again.");
                        break;
                    }
                    case 0:
                        open = false;
                        break;
                    default:
                        error("That option is not on this menu.");
                }
            } catch (LibraryException e) {
                error(e.getMessage());
            }
        }
    }

    private void recommendationMenu() {
        section("RECOMMENDATIONS");
        try {
            String patronId = read("Patron ID: ");
            List<RecommendationStrategy> strategies =
                    app.getRecommendationService().getAvailableStrategies();
            System.out.println("  0. Blended (default)");
            for (int i = 1; i < strategies.size(); i++) {
                System.out.printf("  %d. %s%n", i, strategies.get(i).getStrategyName());
            }
            int pick = readInt("Strategy: ");
            RecommendationStrategy chosen = pick > 0 && pick < strategies.size()
                    ? strategies.get(pick) : strategies.get(0);
            List<LibraryItem> suggestions =
                    app.getRecommendationService().recommendFor(patronId, chosen, 5);
            info("Using: " + chosen.getStrategyName());
            if (suggestions.isEmpty()) {
                info("Not enough history yet to suggest anything.");
            }
            printItems(suggestions);
        } catch (LibraryException e) {
            error(e.getMessage());
        }
    }

    private void auditMenu() {
        section("EVENTS AND AUDIT TRAIL");
        info("Listeners subscribed: " + app.getEventPublisher().getListeners().size());
        app.getEventPublisher().getListeners()
                .forEach(listener -> System.out.println("    " + listener.getListenerName()));
        System.out.println();
        info("Recent events:");
        List<com.library.event.LibraryEvent> history = app.getEventPublisher().getHistory();
        history.stream().skip(Math.max(0, history.size() - 15))
                .forEach(event -> System.out.println("    " + event));
        System.out.println();
        info("Notices delivered: " + app.getNotificationService().getSentCount());
        app.getNotificationService().getSentLog().stream()
                .skip(Math.max(0, app.getNotificationService().getSentCount() - 5))
                .forEach(entry -> System.out.println("    " + entry));
    }

    private void printItems(List<LibraryItem> items) {
        if (items.isEmpty()) {
            info("Nothing matched.");
            return;
        }
        for (LibraryItem item : items) {
            System.out.printf("  %-9s %s%n", "[" + item.getItemType() + "]", item.getSummary());
        }
        info("Total: " + items.size());
    }

    private void printLoans(List<Loan> loans) {
        if (loans.isEmpty()) {
            info("No loans matched.");
            return;
        }
        loans.forEach(loan -> System.out.println("  " + loan));
        info("Total: " + loans.size());
    }

    private Genre readGenre() {
        Genre[] values = Genre.values();
        StringBuilder builder = new StringBuilder("  ");
        for (int i = 0; i < values.length; i++) {
            builder.append(i + 1).append('.').append(values[i].getLabel()).append("  ");
        }
        System.out.println(builder);
        int pick = readInt("Genre: ");
        return pick >= 1 && pick <= values.length ? values[pick - 1] : Genre.FICTION;
    }

    private int readChoice() {
        try {
            return Integer.parseInt(read("").trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int readInt(String prompt) {
        return Validator.parseInt(read(prompt), "number");
    }

    private String read(String prompt) {
        if (!prompt.isEmpty()) {
            System.out.print("  " + prompt);
        }
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            goodbye();
            System.exit(0);
            return "";
        }
    }

    private void section(String title) {
        System.out.println();
        System.out.println(THIN);
        System.out.println("  " + title);
        System.out.println(THIN);
    }

    private void ok(String message) {
        System.out.println("  [OK] " + message);
    }

    private void error(String message) {
        System.out.println("  [ERROR] " + message);
    }

    private void info(String message) {
        System.out.println("  " + message);
    }

    private void goodbye() {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  Closing the library. Goodbye.");
        System.out.println(LINE);
    }
}
