package ru.ifmo.rain.maslov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {

    private static <A, R> Thread threadGenerator(int i, List<R> res, Stream<A> data,
                                                 Function<? super Stream<A>, R> task) {
        return new Thread(() -> res.set(i, task.apply(data)));
    }

    private <A, R> R doJob(int threads, List<A> values,
                           Function<? super Stream<A>, R> task,
                           Function<? super Stream<R>, R> collector)
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
            jobs.set(i, threadGenerator(i, result, values.subList(pos, pos + block).stream(), task));
            jobs.get(i).start();
        }
        InterruptedException exception = null;
        for (Thread i : jobs) {
            try {
                i.join();
            } catch (InterruptedException exceptionJoin) {
                if (exception == null)
                    exception = new InterruptedException("Not all thread joined");
                exception.addSuppressed(exceptionJoin);
                for (Thread j : jobs) {
                    if (j.isAlive()) {
                        j.interrupt();
                    }
                }
            }
        }
        if (exception != null)
            throw exception;
        return collector.apply(result.stream());
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
    public String join(int threads, List<?> values) throws InterruptedException {
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
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
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
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
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
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
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
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values == null || values.isEmpty())
            throw new IllegalArgumentException("values was == null or isEmpty()");
        Function<Stream<? extends T>, T> streamMax = stream -> stream.min(comparator).get();
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
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
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
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
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
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> reducer =
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
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
        return doJob(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
        // return reduce(threads, map(threads, values, lift), monoid);
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.concurrent advanced ru.ifmo.rain.maslov.concurrent.IterativeParallelism

