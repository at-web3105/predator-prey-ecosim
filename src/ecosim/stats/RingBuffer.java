package ecosim.stats;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Fixed-capacity history: once full, adding overwrites the oldest entry.
 * Used to keep the last N samples for the graphs without growing forever.
 *
 * ITERATOR PATTERN: callers loop with for (T t : buffer) oldest-to-newest
 * without knowing the data is really a circular array with a moving head.
 * The iterator is a separate object holding its own position, so two loops
 * over the same buffer do not interfere. It is fail-fast: modifying the
 * buffer mid-loop throws ConcurrentModificationException, like ArrayList.
 */
public final class RingBuffer<T> implements Iterable<T> {

    private final Object[] data;   // generic arrays cannot be created directly (erasure)
    private int head;              // index of the oldest element
    private int size;
    private int modCount;

    public RingBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.data = new Object[capacity];
    }

    public void add(T item) {
        int tail = (head + size) % data.length;
        data[tail] = item;
        if (size < data.length) size++;
        else head = (head + 1) % data.length;
        modCount++;
    }

    public int size()       { return size; }
    public int capacity()   { return data.length; }
    public boolean isEmpty(){ return size == 0; }

    /** i = 0 is the oldest element. */
    @SuppressWarnings("unchecked")  // safe: only T values are ever stored by add()
    public T get(int i) {
        if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i);
        return (T) data[(head + i) % data.length];
    }

    public T newest() {
        if (size == 0) throw new NoSuchElementException("buffer is empty");
        return get(size - 1);
    }

    public void clear() {
        java.util.Arrays.fill(data, null);
        head = size = 0;
        modCount++;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int next = 0;
            private final int expectedMod = modCount;

            @Override public boolean hasNext() { return next < size; }

            @Override public T next() {
                if (modCount != expectedMod) throw new ConcurrentModificationException();
                if (!hasNext()) throw new NoSuchElementException();
                return get(next++);
            }
        };
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
