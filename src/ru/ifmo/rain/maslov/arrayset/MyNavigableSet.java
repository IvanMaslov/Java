package ru.ifmo.rain.maslov.arrayset;

import java.util.*;

public class MyNavigableSet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> elements;
    private final Comparator<? super T> comp;

    public MyNavigableSet() {
        this(Collections.emptyList());
    }

    public MyNavigableSet(Collection<T> collection) {
        this(collection, null);
    }

    public MyNavigableSet(Collection<T> collection, Comparator<? super T> comp) {
        this(new ArrayList<>(collection), comp);
    }

    public MyNavigableSet(List<T> elements, Comparator<? super T> comp) {
        TreeSet<T> tree = new TreeSet<T>(comp);
        tree.addAll(elements);
        this.elements = new ArrayList<>(tree);
        this.comp = comp;
    }

    private MyNavigableSet(List<T> elements, Comparator<? super T> comp, boolean sort) {
        if (sort) {
            elements.sort(comp);
        }
        this.elements = elements;
        this.comp = comp;
    }

    private void check() {
        if (elements == null || elements.size() == 0)
            throw new NoSuchElementException();
    }

    private T getElem(int index) {
        if (index < 0 || index >= size())
            return null;
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
    public MyNavigableSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public MyNavigableSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public MyNavigableSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
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
    public MyNavigableSet<T> descendingSet() {
        return new MyNavigableSet<>(new ReverseList<>(true, elements),
                comp == null ? Collections.reverseOrder() : comp.reversed());
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public MyNavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (comp == null
                && fromElement instanceof Comparable
                && ((Comparable) fromElement).compareTo(toElement) > 0)
            throw new IllegalArgumentException();
        if (comp != null && comp.compare(fromElement, toElement) > 0)
            throw new IllegalArgumentException();
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    @Override
    public MyNavigableSet<T> headSet(T toElement, boolean inclusive) {
        int to = Collections.binarySearch(elements, toElement, comp);
        if (to < 0) to = ~to;
        else if (inclusive) ++to;
        if (to < 0) return new MyNavigableSet<>(new ArrayList<>(), comp);
        return new MyNavigableSet<T>(elements.subList(0, to), comp, false);
    }

    @Override
    public MyNavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int from = Collections.binarySearch(elements, fromElement, comp);
        if (from < 0) from = ~from;
        else if (!inclusive) ++from;
        if (from > elements.size()) return new MyNavigableSet<>(new ArrayList<>(), comp);
        return new MyNavigableSet<T>(elements.subList(from, elements.size()), comp, false);
    }

    class ReverseList<T> extends AbstractList<T> {
        private boolean reversed;
        final private List<T> elements;

        ReverseList(boolean reversed, List<T> elements) {
            this.reversed = reversed;
            this.elements = elements;
        }

        public void reverse() {
            reversed = !reversed;
        }

        @Override
        public T get(int index) {
            return reversed ? elements.get(index) : elements.get(elements.size() - 1 - index);
        }

        @Override
        public int size() {
            return elements.size();
        }
    }
}
