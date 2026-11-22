public class SinglyLinkedList {

    private static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
            this.next = null;
        }
    }

    private Node head;
    private int count;
    private int capacity;

    public SinglyLinkedList(int capacity) {
        this.capacity = capacity;
        head = null;
        count = 0;
    }

    public boolean addFirst(String value) {
        if (isFull()) {
            return false;
        }
        Node node = new Node(value);
        node.next = head;
        head = node;
        count++;
        return true;
    }

    public boolean addLast(String value) {
        if (isFull()) {
            return false;
        }
        Node node = new Node(value);
        if (head == null) {
            head = node;
        } else {
            Node current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = node;
        }
        count++;
        return true;
    }
 
    public boolean addAt(String value, int position) {
        if (isFull() || position < 1 || position > count + 1) {
            return false;
        }
        if (position == 1) {
            return addFirst(value);
        }
        Node before = head;
        for (int i = 1; i < position - 1; i++) {
            before = before.next;
        }
        Node node = new Node(value);
        node.next = before.next;
        before.next = node;
        count++;
        return true;
    }

    public String removeFirst() {
        if (head == null) {
            return null;
        }
        String removed = head.value;
        head = head.next;
        count--;
        return removed;
    }

    public int remove(String value) {
        Node previous = null;
        Node current = head;
        int index = 0;
        while (current != null) {
            if (current.value.equals(value)) {
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                count--;
                return index;
            }
            previous = current;
            current = current.next;
            index++;
        }
        return -1;
    }

    public int find(String value) {
        Node current = head;
        int index = 0;
        while (current != null) {
            if (current.value.equals(value)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public boolean isFull() {
        return count == capacity;
    }

    public int size() {
        return count;
    }

    public String[] toArray() {
        String[] copy = new String[count];
        Node current = head;
        int i = 0;
        while (current != null) {
            copy[i] = current.value;
            i++;
            current = current.next;
        }
        return copy;
    }
}
