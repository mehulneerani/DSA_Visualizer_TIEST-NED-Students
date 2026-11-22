public class Stack {
    private String[] items;
    private int count;
 
    public Stack(int capacity) {
        items = new String[capacity];
        count = 0;
    }

    public boolean push(String value) {
        if (isFull()) {
            return false;
        }
        items[count] = value;
        count++;
        return true;
    }

    public String pop() {
        if (isEmpty()) {
            return null;
        }
        count--;
        String top = items[count];
        items[count] = null;
        return top;
    }

    public String peek() {
        if (isEmpty()) {
            return null;
        }
        return items[count - 1];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == items.length;
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
