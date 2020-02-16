package ru.ifmo.rain.maslov.arrayset;

import java.util.*;

public class MyNavigableSet<T> extends MySortedSet<T> implements NavigableSet<T> {

    public MyNavigableSet() {
        super();
    }

    public MyNavigableSet(Collection<T> collection) {
        super(collection);
    }

    public MyNavigableSet(Collection<T> collection, Comparator<? super T> comp) {
        super(collection, comp);
    }

    public MyNavigableSet(List<T> elements, Comparator<? super T> comp) {
        super(elements, comp);
    }

    private T getElem(int index) {
        // super.check();
        if(index < 0 || index >= size())
            return null;
        return elements.get(index);
    }

    private int indexCalc(T value, int findOffset, int notFindOffset) {
        int u = Collections.binarySearch(elements, value, comp);
        if(u < 0)
            return ~u + notFindOffset;
        return u + findOffset;
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
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        ArrayList<T> rev = new ArrayList<>(elements);
        Collections.reverse(rev);
        return new MyNavigableSet<>(rev, Collections.reverseOrder(comp));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    @Override
    public MyNavigableSet<T> headSet(T toElement, boolean inclusive) {
        int to = Collections.binarySearch(elements, toElement, comp);
        if (to < 0) to = ~to;
        else if(inclusive) ++to;
        return new MyNavigableSet<>(elements.subList(0, to), comp);
    }

    @Override
    public MyNavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int from = Collections.binarySearch(elements, fromElement, comp);
        if (from < 0) from = ~from;
        else if (!inclusive) ++from;
        return new MyNavigableSet<>(elements.subList(from, elements.size()), comp);
    }
}
