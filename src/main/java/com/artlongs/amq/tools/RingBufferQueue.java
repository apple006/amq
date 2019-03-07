package com.artlongs.amq.tools;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Func : RingBuffer Queue, 自动扩容
 *
 * @author: leeton on 2019/2/26.
 */
public class RingBufferQueue<T> implements Iterable {
    private final static int DEFAULT_SIZE = 256;
    private T[] items;
    private int head = 0;
    private int tail = 0;
    private int capacity = DEFAULT_SIZE;
    private int realSize = 0; //实际的元素个数
    private final AtomicInteger lock = new AtomicInteger();

    public RingBufferQueue() {
        this.capacity = DEFAULT_SIZE;
        this.items = (T[]) Array.newInstance(Object.class, DEFAULT_SIZE);
    }

    /**
     * capacity 容量一定要设定为2的倍数
     *
     * @param capacity
     */
    public RingBufferQueue(int capacity) {
        if (capacity <= 0) capacity = DEFAULT_SIZE;
        if (capacity % 2 != 0) throw new UnsupportedOperationException("RBQ(capacity),容量一定要设定为2的倍数.");
        this.capacity = capacity;
        this.items = (T[]) Array.newInstance(Object.class, capacity);
    }

    public Boolean empty() {
        return head == tail;
    }
    public Boolean notEmpty() {
        return !empty();
    }

    public Boolean full() {
        return (tail + 1) % capacity == head;
    }

    public void clear() {
        lock.getAndIncrement();
        Arrays.fill(items, null);
        this.head = 0;
        this.tail = 0;
    }

    public int size() {
        return (realSize = tail - head);
    }

    public int capacity() {
        return capacity;
    }

    public int put(T v) {
        lock.getAndIncrement();
        if (full()) {
            extendCapacity();
        }
        final int current = tail;
        items[current] = v;
        tail = (tail + 1) % capacity;
        size();
        return current;
    }

    /**
     * Get and remove element from head
     *
     * @return
     */
    public T pop() {
        if (empty()) {
            return null;
        }
        lock.getAndIncrement();
        T item = items[head];
        remove(head);
        head = (head + 1) % capacity;
        size();
        return item;
    }

    public T getOf(int index) {
        if (index >= size() || index < 0) throw new UnsupportedOperationException("RingBufferQueue,队列上标或下标溢出.");
        T item = items[index];
        return item;
    }

    public Result putIfAbsent(T v) {
        boolean found = false;
        int oldIndex = 0;
        int _index = 0;
        final Iterator iter = iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o!=null && o.equals(v)) {
                found = true;
                oldIndex = _index;
                break;
            }
            _index++;
        }
        if (!found) {
            return new Result(true, put(v));
        }
        return new Result(false, oldIndex);
    }

    /**
     * 实际是把值设为 NULL,但 BOX 还是存在的.
     *
     * @param index
     */
    public void remove(int index) {
        lock.getAndIncrement();
        if (index < 0) throw new UnsupportedOperationException("RingBufferQueue,队列上标溢出.");
        items[index] = null;
    }

    /**
     * 扩容
     */
    private void extendCapacity() {
        lock.getAndIncrement();
        final int oldCapacity = items.length;
        final int newCapacity = capacity << 1; //扩容一倍
        final T[] newElementData = (T[]) Array.newInstance(Object.class, newCapacity);
        System.arraycopy(items, 0, newElementData, 0, oldCapacity);
        this.items = newElementData;
        this.capacity = newCapacity;
    }

    public T[] asArray(Class<?> clazz) {
        T[] newItems = (T[]) Array.newInstance(clazz, size());
        System.arraycopy(items, 0, newItems, 0, size());
        return newItems;
    }

    public List<T> asList() {
        final List<T> list = new ArrayList<>(size());
        if (empty()) return list;
        for (T item : items) {
            list.add(item);
        }
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index;

            @Override
            public boolean hasNext() { // 因为是队列,为 true 时,值也可能为 null , 这里只是判断是否存在这个 box
                return index < size();
            }

            @Override
            public T next() {
                if (index < size()) {
                    return items[index++];
                }
                return null;
            }
        };
    }

    public void remove(T v) {
        for (int i = 0; i < items.length; i++) {
            if (v.equals(items[i])) {
                remove(i);
                break;
            }
        }
    }

    public static class Result{
        public boolean success;
        public int index; // 在队列里存放的位置

        public Result(boolean success, int index) {
            this.success = success;
            this.index = index;
        }
    }

    public static void main(String[] args) {
        RingBufferQueue<ByteBuffer> rbq = new RingBufferQueue<>(2);
        rbq.put(ByteBuffer.wrap("hello".getBytes()));
        rbq.put(ByteBuffer.wrap("world".getBytes()));
        rbq.put(ByteBuffer.wrap("moon!".getBytes()));
        rbq.putIfAbsent(ByteBuffer.wrap("moon!".getBytes()));
        System.err.println(rbq.size());
//        rbq.pop();

        Iterator<ByteBuffer> iter = rbq.iterator();
        while (iter.hasNext()) {
            System.err.println(new String(iter.next().array()));
        }

        for (ByteBuffer o : rbq.asArray(ByteBuffer.class)) {
            System.err.println(new String(o.array()));
        }

    }


}
