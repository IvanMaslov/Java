package ru.ifmo.rain.maslov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {

    private static <A, R> R doJob(int threads, final List<A> values,
                                  final Function<? super Stream<A>, R> task,
                                  final Function<? super Stream<R>, R> collector)
            throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("thread number was negative");
        }
        threads = Math.min(threads, values.size());
        final List<Thread> jobs = new ArrayList<>(Collections.nCopies(threads, null));
        final List<R> result = new ArrayList<>(Collections.nCopies(threads, null));
        final int rest = values.size() % threads;
        int block = 1 + values.size() / threads;
        for (int i = 0, pos = 0; i < threads; ++i, pos += block) {
            if (rest == i)
                block -= 1;
            final int _i = i;
            final int _pos = pos;
            final int _block = block;
            jobs.set(i, new Thread(() -> result.set(_i, task.apply(values.subList(_pos, _pos + _block).stream()))));
            jobs.get(i).start();
        }
        InterruptedException exception = null;
        for (final Thread i : jobs) {
            try {
                i.join();
            } catch (final InterruptedException exceptionJoin) {
                if (exception == null) {
                    exception = new InterruptedException("Not all thread joined");
                    for (final Thread j : jobs) {
                        j.interrupt();
                    }
                }
                exception.addSuppressed(exceptionJoin);
            }
        }
        if (exception != null) {
            throw exception;
        }
        return collector.apply(result.stream());
    }

    private <T> Function<Stream<T>, T> monoidReducer(final Monoid<T> monoid) {
        return stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    private <T, U> List<U> doStreamAction(final int threads, final List<? extends T> values,
                                          final Function<Stream<? extends T>, Stream<? extends U>> action) throws InterruptedException {
        return doJob(threads, values,
                stream -> action.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return doStreamAction(threads, values, stream -> stream.filter(predicate));
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return doStreamAction(threads, values, stream -> stream.map(f));
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values == null || values.isEmpty())
            throw new IllegalArgumentException("values was == null or isEmpty()");
        final Function<Stream<? extends T>, T> streamMax = stream -> stream.min(comparator).get();
        return doJob(threads, values, streamMax, streamMax);
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        final Function<Stream<T>, T> reducer = monoidReducer(monoid);
        return doJob(threads, values, reducer, reducer);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param lift    mapping function.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        final Function<Stream<R>, R> reducer = monoidReducer(monoid);
        return doJob(threads, values, stream -> reducer.apply(stream.map(lift)), reducer);
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.concurrent advanced ru.ifmo.rain.maslov.concurrent.IterativeParallelism

