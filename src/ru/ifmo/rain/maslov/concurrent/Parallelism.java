package ru.ifmo.rain.maslov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parallelism implements ListIP {

    private static <A, R> Thread threadGenerator(int i, List<R> res, Stream<? extends A> data,
                                                 Function<? super Stream<? extends A>, ? extends R> task) {
        return new Thread(() -> res.set(i, task.apply(data)));
    }

    private <A, R> R doJob(int threads, List<? extends A> values,
                           Function<? super Stream<? extends A>, ? extends R> task,
                           Function<? super Stream<? extends R>, ? extends R> collector)
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
        for (Thread i : jobs) {
            i.join();
        }
        return collector.apply(result.stream());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values == null || values.isEmpty())
            throw new IllegalArgumentException("values was == null or isEmpty()");
        Function<Stream<? extends T>, ? extends T> streamMax = stream -> stream.min(comparator).get();
        return doJob(threads, values, streamMax, streamMax);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doJob(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.concurrent list ru.ifmo.rain.maslov.concurrent.Parallelism

