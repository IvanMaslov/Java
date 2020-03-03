package ru.ifmo.rain.maslov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> elements;
    private final Comparator<? super T> comp;

    public ArraySet() {
        this(Collections.emptyList());
    }

    public ArraySet(Collection<T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<T> collection, Comparator<? super T> comp) {
        this(new ArrayList<>(collection), comp);
    }

    public ArraySet(List<T> elements, Comparator<? super T> comp) {
        this(elements, comp, true);
    }

    private ArraySet(List<T> elements, Comparator<? super T> comp, boolean sort) {
        if (sort) {
            TreeSet<T> tree = new TreeSet<>(comp);
            tree.addAll(elements);
            elements = new ArrayList<>(tree);
        } else {
            if (elements instanceof ReverseList
                    && ((ReverseList<T>) elements).elements instanceof ReverseList)
                elements = ((ReverseList<T>) ((ReverseList<T>) elements).elements).elements;
        }
        this.elements = elements;
        this.comp = comp;
    }

    private void check() {
        if (elements == null || elements.size() == 0) {
            throw new NoSuchElementException();
        }
    }

    private T getElem(int index) {
        check();
        if (index < 0 || index >= size()) {
            return null;
        }
        return elements.get(index);
    }

    private int indexCalc(T value, int findOffset, int notFindOffset) {
        int u = Collections.binarySearch(elements, value, comp);
        if (u < 0)
            return ~u + notFindOffset;
        return u + findOffset;
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
    public ArraySet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public ArraySet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public ArraySet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        check();
        return getElem(0);
    }

    @Override
    public T last() {
        check();
        return getElem(elements.size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(elements, (T) Objects.requireNonNull(o), comp) >= 0;
    }

    @Override
    public T lower(T t) {
        return getElem(indexCalc(t, -1, -1));
    }

    @Override
    public T floor(T t) {
        return getElem(indexCalc(t, 0, -1));
    }

    @Override
    public T ceiling(T t) {
        return getElem(indexCalc(t, 0, 0));
    }

    @Override
    public T higher(T t) {
        return getElem(indexCalc(t, 1, 0));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArraySet<T> descendingSet() {
        return new ArraySet<>((new ReverseList<>(elements)),
                comp == null ? Collections.reverseOrder() : comp.reversed(), false);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArraySet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if ((comp == null
                && fromElement instanceof Comparable
                && ((Comparable) fromElement).compareTo(toElement) > 0)
                || (comp != null && comp.compare(fromElement, toElement) > 0))
            throw new IllegalArgumentException();
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    private ArraySet<T> emptySet() {
        return new ArraySet<>(new ArrayList<>(), comp);
    }

    private ArraySet<T> subSet(int from, int to) {
        return new ArraySet<>(elements.subList(from, to), comp, false);
    }

    @Override
    public ArraySet<T> headSet(T toElement, boolean inclusive) {
        int to = indexCalc(toElement, inclusive ? 1 : 0, 0);
        return to < 0 ? emptySet() : subSet(0, to);
    }

    @Override
    public ArraySet<T> tailSet(T fromElement, boolean inclusive) {
        int from = indexCalc(fromElement, inclusive ? 0 : 1, 0);
        return from > size() ? emptySet() : subSet(from, size());
    }

    class ReverseList<D> extends AbstractList<D> {
        final private List<D> elements;

        ReverseList(List<D> elements) {
            this.elements = elements;
        }

        @Override
        public D get(int index) {
            return elements.get(elements.size() - 1 - index);
        }

        @Override
        public int size() {
            return elements.size();
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.arrayset NavigableSet ru.ifmo.rain.maslov.arrayset.ArraySet main
