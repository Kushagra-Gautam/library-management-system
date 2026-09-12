# Design Document

Supplementary notes on the decisions behind the Library Management System. The README covers what the
system does and which principles it applies; this document records *why* each choice was made and
what was rejected.

---

## 1. Domain model: the catalogue/copy split

### The decision

`LibraryItem` (a catalogue record) and `ItemCopy` (a physical object on a shelf) are separate types.

### What was rejected

A single `Book` class with `available` and `branch` fields. It is simpler, and it works until the
library owns two copies of anything — at which point `available` is meaningless, because the answer
is "two of the four are".

### What the split buys

| Question | Answered by |
|---|---|
| "What did Robert C. Martin write?" | `LibraryItem` — catalogue level |
| "Can I borrow it today?" | `ItemCopy` — three available, one on loan |
| "Where is it?" | `ItemCopy.branchId` — copies in different cities |
| "I want it when it comes back" | `Reservation` on the *work*, satisfied by *any* copy |

Multi-branch support and the hold queue, both listed as optional extensions, needed almost no extra
machinery once this split existed. They are expressions of copy state.

### The cost

Two objects where a naive design has one, and a layer of indirection when answering "is this
available" — `InventoryService` counts copies by status rather than reading a boolean. That is the
right trade: the count is derived, so it cannot drift out of step with reality the way a cached
boolean would.

---

## 2. Why `Magazine` exists

It could have been cut — the brief only mentions books. It stays because it is the only way to
*prove* the Liskov substitution principle rather than assert it.

An abstraction with one implementation is untested. Adding a second format immediately exposed two
places where the design had quietly assumed "book":

1. An early `SearchStrategy` signature took `Collection<Book>`. Generalising it to `LibraryItem`
   forced `getContributor()` onto the base class — which turned out to be the right abstraction
   anyway, since "the person or body credited with the work" is an author for a book and a publisher
   for a magazine.
2. An early `Book.getSummary()` was called directly by the console. Making it abstract on
   `LibraryItem` moved the formatting decision to where the data lives.

Both changes improved the design, and neither would have surfaced with a single format.

---

## 3. Strategy, applied three times

The pattern appears for search, lending terms and recommendations. The shared justification: in each
case there is a *family* of interchangeable algorithms, chosen at runtime, and the number of family
members is expected to grow.

The counter-example is instructive. `Validator` is a static utility, **not** a strategy, because
there is exactly one way to validate an ISBN and no expectation of a second. Making it a strategy
would be pattern-for-its-own-sake — indirection with nothing on the other end.

### Where each variant differs

- **Search** returns a filtered list; the composite unions results.
- **`LoanPolicy`** returns scalars, with `calculateFine` as a `default` method that
  `PremiumLoanPolicy` overrides to add a grace period. A default method was chosen over an abstract
  one so most policies get correct behaviour for free.
- **Recommendations** return a *ranked* list, which is why the composite scores by position rather
  than simply unioning.

---

## 4. Observer: what it prevents

Consider `returnCopy` without the pattern:

```java
// rejected design
public Loan returnCopy(String barcode) {
    // ... close the loan, free the copy ...
    reservationService.fulfilNext(itemId, barcode);
    notificationService.notify(patron, "Ready", "...");
    auditLog.record("returned", barcode);
    recommendationCache.invalidate(patron);
}
```

Every new reaction to a return edits this method, and `LendingService` accumulates dependencies on
notifications, auditing and caching. With the event bus, the method publishes `BOOK_RETURNED` and
stops. New reactions are new listeners.

### Two deliberate details

**Listeners declare their own interests** through `isInterestedIn(EventType)`. The publisher does no
filtering, so a listener never receives events it would only discard, and the filter lives next to
the code that cares about it.

**A throwing listener is logged and skipped.** A failing audit writer must not roll back a completed
book return. The transaction has already happened; the observer is a consequence, not a participant.

---

## 5. The reservation flow

The most intricate interaction in the system, and the one where the patterns pay off visibly:

```
LendingService.returnCopy(barcode)
  |
  +-- close the loan, charge any fine
  +-- mark the copy AVAILABLE
  +-- publish BOOK_RETURNED  ------> AuditTrailListener  (records it)
  |
  +-- reservationService.fulfilNext(itemId, barcode)
        |
        +-- peek the queue (empty? stop)
        +-- mark the copy ON_HOLD      <-- not AVAILABLE
        +-- mark the reservation READY
        +-- publish RESERVATION_READY ------> NotificationListener
                                                |
                                                +-- NotificationService
                                                      +-- channel by patron preference
```

`LendingService` knows about `ReservationService` (through an optional setter). It knows **nothing**
about notifications. `ReservationService` knows nothing about notifications either — it publishes an
event. The notification listener knows nothing about SMS or email — it calls a channel interface.

Each layer knows only the next one down, and the chain can be extended at any link without touching
the others.

### Why `ON_HOLD` rather than `AVAILABLE`

If the returned copy were shelved as available, a walk-in patron could borrow it before the person
who has waited three weeks collects it. `ON_HOLD` is invisible to `findFirstAvailable`, so the copy
is reserved in the literal sense. The test suite asserts that a third patron is refused.

---

## 6. The circular dependency, and how it is broken

Lending needs reservations (a return fulfils a hold; a renewal must check the queue). Reservations
need inventory, which lending also uses. Constructor injection in both directions cannot be
satisfied — neither object can be built first.

Three options were considered:

| Option | Verdict |
|---|---|
| Merge the two services | Rejected — one class with two reasons to change |
| A mediator between them | Rejected — a third class whose only job is to hold a reference |
| Optional setter on lending | **Chosen** |

The setter breaks the cycle and has a genuine benefit: `if (reservationService != null)` means
lending works correctly with the reservation module absent entirely. The optional feature is
optional in the code, not just in the brief.

---

## 7. Identifiers: natural versus surrogate

**ISBN is the catalogue key** — a natural key. It is globally unique, externally meaningful, and
already printed on the object. Generating a surrogate ID alongside it would mean two identifiers for
one thing and a lookup table between them.

**Patrons, loans, reservations and copies get generated keys** (`P-001`, `L-001`, `R-001`,
`BC-00001`). None has a natural identifier: names repeat, and email addresses change.

`equals`/`hashCode` follow the key in both cases, with a `getClass()` check so a `Book` never equals
a `Magazine` that happens to share an identifier.

---

## 8. Where mutability was allowed, and where it was not

The system is not uniformly immutable, which was deliberate.

**Immutable:** `LibraryEvent` — an event is a historical fact, and a listener must not be able to
mutate domain state through the event it was handed. Its payload map is defensively copied in and
exposed unmodifiable.

**Read-only views:** `Patron.getBorrowingHistory()` returns `Collections.unmodifiableList`, because
history is appended through `recordBorrowing` under the service's control and must not be rewritten
from outside. Every repository `findAll()` returns a copy, so a caller cannot mutate the store.

**Mutable:** entities like `ItemCopy` and `Loan` change state legitimately — a copy is borrowed, a
loan is returned. Making them immutable would mean replacing objects on every transition and an
identity map to track which version is current, which buys nothing for a single-threaded console
application.

---

## 9. Logging levels, chosen deliberately

| Level | Used for | Example |
|---|---|---|
| `INFO` | Business events worth an audit record | loan created, copy transferred, patron registered |
| `FINE` | Diagnostic detail | which search strategy ran, how many suggestions were produced |
| `WARNING` | Rejected operations and failed listeners | duplicate ISBN, borrowing limit reached |

Messages that cost something to build use lambda suppliers:

```java
LOGGER.fine(() -> String.format("Search [%s] for '%s' returned %d result(s)", ...));
```

The lambda is not invoked when the level is disabled, so a `FINE` call in a hot path costs nothing in
production.

The console handler defaults to `INFO` while the file handler records everything, so the operator
sees a readable stream and the full trail survives in `logs/library.log`.

---

## 10. Testing approach

No JUnit, because the brief excludes external dependencies. `LibraryTestRunner` is a small harness:
`check(description, condition)` records a pass or failure, and a summary prints at the end with a
non-zero exit code on failure, so `--test` can gate a build.

Each group builds a fresh `LibraryApplication`. That is worth noting as evidence rather than
convenience: it is only possible because no service reaches for a singleton or a static store. A
design with global state would force tests to run in order and clean up after each other.

The 136 checks deliberately cover the *rules*, not just the happy paths — a blocked patron refused, a
held copy refused, an in-transit copy refused, a double reservation refused, a renewal refused while
someone waits. Those are where the design would break first.

---

## 11. What was left out, and why

| Omitted | Reason |
|---|---|
| Persistence | Explicitly out of scope per the brief |
| Concurrency control | A console application is single-threaded; adding locks would be speculative |
| Fine payment | Fines are calculated and recorded, but a payment ledger is a separate bounded context |
| Reservation expiry | The natural next feature — a hold uncollected within *n* days returns to the shelf. The status enum already has `EXPIRED` reserved for it. |
| User accounts and permissions | The brief describes a librarian's tool with one class of operator |

Each of these is a place where a real system would grow. None of them would require restructuring
what is here, which is the point of the layering.
