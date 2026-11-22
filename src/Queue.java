public class Queue {
    private String[] items;
    private int count;
 
    public Queue(int capacity) {
        items = new String[capacity];
        count = 0;
    }

    public boolean enqueue(String value) {
        if (count == items.length) {
            return false;
        }
        items[count] = value;
        count++;
        return true;
    }
 
    public String dequeue() {
        if (count == 0) {
            return null;
        }
        String first = items[0];
        for (int i = 1; i < count; i++) {
            items[i - 1] = items[i];
        }
        count--;
        items[count] = null;
        return first;
    }

    public String front() {
        if (count == 0) {
            return null;
        }
        return items[0];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int size() {
        return count;
    }

    public String[] toArray() {
        String[] copy = new String[count];
        for (int i = 0; i < count; i++) {
            copy[i] = items[i];
        }
        return copy;
    }
}
