package com.library.repository.inmemory;

import com.library.repository.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Shared in-memory implementation of {@link Repository}.
 *
 * <p>A {@link LinkedHashMap} gives constant time lookup by identifier while
 * preserving insertion order, so listings are stable between runs. Subclasses
 * supply only the function that extracts an identifier from an entity; every
 * generic operation is inherited.</p>
 *
 * @param <T> the entity type held
 * @param <I> the type of that entity's identifier
 */
public abstract class InMemoryRepository<T, I> implements Repository<T, I> {

    private final Map<I, T> store = new LinkedHashMap<>();
    private final Function<T, I> idExtractor;

    protected InMemoryRepository(Function<T, I> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public T save(T entity) {
        store.put(idExtractor.apply(entity), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(I id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean deleteById(I id) {
        return store.remove(id) != null;
    }

    @Override
    public boolean existsById(I id) {
        return store.containsKey(id);
    }

    @Override
    public int count() {
        return store.size();
    }

    /**
     * @param predicate the test each entity must pass
     * @return every entity satisfying the predicate
     */
    protected List<T> filter(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * @param predicate the test to apply
     * @return the first entity satisfying the predicate, in insertion order
     */
    protected Optional<T> firstMatching(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).findFirst();
    }
}
