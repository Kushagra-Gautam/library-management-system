package com.library.app;

import com.library.event.AuditTrailListener;
import com.library.event.EventPublisher;
import com.library.event.NotificationListener;
import com.library.notification.NotificationService;
import com.library.policy.LoanPolicyResolver;
import com.library.recommendation.AuthorAffinityStrategy;
import com.library.recommendation.CollaborativeFilteringStrategy;
import com.library.recommendation.CompositeRecommendationStrategy;
import com.library.recommendation.GenreAffinityStrategy;
import com.library.recommendation.PopularityStrategy;
import com.library.repository.BranchRepository;
import com.library.repository.CopyRepository;
import com.library.repository.ItemRepository;
import com.library.repository.LoanRepository;
import com.library.repository.PatronRepository;
import com.library.repository.ReservationRepository;
import com.library.repository.inmemory.InMemoryBranchRepository;
import com.library.repository.inmemory.InMemoryCopyRepository;
import com.library.repository.inmemory.InMemoryItemRepository;
import com.library.repository.inmemory.InMemoryLoanRepository;
import com.library.repository.inmemory.InMemoryPatronRepository;
import com.library.repository.inmemory.InMemoryReservationRepository;
import com.library.service.CatalogService;
import com.library.service.InventoryService;
import com.library.service.LendingService;
import com.library.service.PatronService;
import com.library.service.RecommendationService;
import com.library.service.ReservationService;
import com.library.service.TransferService;
import com.library.service.impl.DefaultCatalogService;
import com.library.service.impl.DefaultInventoryService;
import com.library.service.impl.DefaultLendingService;
import com.library.service.impl.DefaultPatronService;
import com.library.service.impl.DefaultRecommendationService;
import com.library.service.impl.DefaultReservationService;
import com.library.service.impl.DefaultTransferService;

/**
 * The composition root: the one place that names concrete classes.
 *
 * <p>Every service is constructed here and handed its collaborators as
 * interfaces. Because the wiring lives in exactly one class, swapping the
 * in-memory repositories for a database means editing the six lines below and
 * nothing else in the system. No service ever calls {@code new} on a repository,
 * which is what makes the dependency inversion principle real here rather than
 * decorative.</p>
 */
public class LibraryApplication {

    private final ItemRepository itemRepository = new InMemoryItemRepository();
    private final PatronRepository patronRepository = new InMemoryPatronRepository();
    private final CopyRepository copyRepository = new InMemoryCopyRepository();
    private final LoanRepository loanRepository = new InMemoryLoanRepository();
    private final ReservationRepository reservationRepository = new InMemoryReservationRepository();
    private final BranchRepository branchRepository = new InMemoryBranchRepository();

    private final EventPublisher eventPublisher = new EventPublisher();
    private final NotificationService notificationService = new NotificationService();
    private final AuditTrailListener auditTrailListener = new AuditTrailListener();
    private final LoanPolicyResolver policyResolver = new LoanPolicyResolver();

    private final CatalogService catalogService;
    private final PatronService patronService;
    private final InventoryService inventoryService;
    private final LendingService lendingService;
    private final ReservationService reservationService;
    private final TransferService transferService;
    private final RecommendationService recommendationService;

    public LibraryApplication() {
        catalogService = new DefaultCatalogService(itemRepository, eventPublisher);
        patronService = new DefaultPatronService(patronRepository, eventPublisher);
        inventoryService = new DefaultInventoryService(copyRepository, itemRepository,
                branchRepository, eventPublisher);
        transferService = new DefaultTransferService(copyRepository, branchRepository, eventPublisher);

        DefaultLendingService lending = new DefaultLendingService(loanRepository, copyRepository,
                itemRepository, patronService, policyResolver, eventPublisher);
        reservationService = new DefaultReservationService(reservationRepository, copyRepository,
                itemRepository, patronService, inventoryService, eventPublisher);
        lending.setReservationService(reservationService);
        lendingService = lending;

        recommendationService = buildRecommendationService();

        eventPublisher.subscribe(auditTrailListener);
        eventPublisher.subscribe(new NotificationListener(notificationService, patronRepository));
    }

    /**
     * Blends four signals, weighted so a patron's own history counts for more
     * than what the library as a whole is reading.
     */
    private RecommendationService buildRecommendationService() {
        CompositeRecommendationStrategy blended = new CompositeRecommendationStrategy()
                .add(new GenreAffinityStrategy(), 3.0)
                .add(new AuthorAffinityStrategy(), 2.5)
                .add(new CollaborativeFilteringStrategy(patronRepository), 2.0)
                .add(new PopularityStrategy(loanRepository), 1.0);

        DefaultRecommendationService service =
                new DefaultRecommendationService(itemRepository, patronService, blended);
        service.registerStrategy(new GenreAffinityStrategy());
        service.registerStrategy(new AuthorAffinityStrategy());
        service.registerStrategy(new CollaborativeFilteringStrategy(patronRepository));
        service.registerStrategy(new PopularityStrategy(loanRepository));
        return service;
    }

    public CatalogService getCatalogService() {
        return catalogService;
    }

    public PatronService getPatronService() {
        return patronService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public LendingService getLendingService() {
        return lendingService;
    }

    public ReservationService getReservationService() {
        return reservationService;
    }

    public TransferService getTransferService() {
        return transferService;
    }

    public RecommendationService getRecommendationService() {
        return recommendationService;
    }

    public EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public AuditTrailListener getAuditTrailListener() {
        return auditTrailListener;
    }

    public LoanPolicyResolver getPolicyResolver() {
        return policyResolver;
    }
}
