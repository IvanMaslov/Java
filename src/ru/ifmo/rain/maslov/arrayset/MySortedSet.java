package ru.ifmo.rain.maslov.arrayset;

import java.util.*;

public class MySortedSet<T> extends AbstractSet<T> implements SortedSet<T> {

    protected final ArrayList<T> elements;
    protected final Comparator<? super T> comp;

    public MySortedSet() {
        this(Collections.emptyList());
    }

    public MySortedSet(Collection<T> collection) {
        this(collection, null);
    }

    public MySortedSet(Collection<T> collection, Comparator<? super T> comp) {
        this(new ArrayList<>(collection), comp);
    }

    public MySortedSet(List<T> elements, Comparator<? super T> comp) {
        TreeSet<T> tree = new TreeSet<T>(comp);
        tree.addAll(elements);
        this.elements = new ArrayList<>(tree);
        this.comp = comp;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comp;
    }

    @Override
    public MySortedSet<T> subSet(T fromElement, T toElement) {
        if (comp.compare(fromElement, toElement) > 0)
            throw new NoSuchElementException();
        return headSet(toElement).tailSet(fromElement);
    }

    @Override
    public MySortedSet<T> headSet(T toElement) {
        int to = Collections.binarySearch(elements, toElement, comp);
        if (to < 0) to = ~to;
        return new MySortedSet<>(elements.subList(0, to), comp);
    }

    @Override
    public MySortedSet<T> tailSet(T fromElement) {
        int from = Collections.binarySearch(elements, fromElement, comp);
        if (from < 0) from = ~from;
        return new MySortedSet<>(elements.subList(from, elements.size()), comp);
    }

    protected void check() {
        if (elements == null || elements.size() == 0)
            throw new NoSuchElementException();
    }

    @Override
    public T first() {
        check();
        return elements.get(0);
    }

    @Override
    public T last() {
        check();
        return elements.get(elements.size() - 1);
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(elements, (T) Objects.requireNonNull(o), comp) >= 0;
    }
}
