import java.util.ArrayList;

public class BinarySearchTree {
 
    public static class Node {
        public String value;
        public Node left;
        public Node right;

        public Node(String value) {
            this.value = value; 
        }
    }

    private Node root;
    private int count;
    private int capacity;
    private ArrayList<String> path = new ArrayList<String>();
    private String deleteType;
    private String successor;
    private ArrayList<String> successorPath = new ArrayList<String>();

    public BinarySearchTree(int capacity) {
        this.capacity = capacity;
        root = null;
        count = 0;
    }

    private int compare(String a, String b) {
        return a.compareToIgnoreCase(b);
    }

    public String insert(String value) {
        path.clear();
        if (count == capacity) {
            return "full";
        }
        int before = count;
        root = insert(root, value);
        if (count == before) {
            return "duplicate";
        }
        return "ok";
    }

    private Node insert(Node node, String value) {
        if (node == null) {
            count++;
            return new Node(value);
        }
        path.add(node.value);
        int result = compare(value, node.value);
        if (result < 0) {
            node.left = insert(node.left, value);
        } else if (result > 0) {
            node.right = insert(node.right, value);
        }
        return node;
    }

    public boolean search(String value) {
        path.clear();
        return search(root, value);
    }

    private boolean search(Node node, String value) {
        if (node == null) {
            return false;
        }
        path.add(node.value);
        int result = compare(value, node.value);
        if (result == 0) {
            return true;
        }
        if (result < 0) {
            return search(node.left, value);
        }
        return search(node.right, value);
    }

    public String findMin() {
        path.clear();
        if (root == null) {
             return null;
        }
        Node current = root;
        path.add(current.value);
        while (current.left != null) {
            current = current.left;
            path.add(current.value);
        }
        return current.value;
    }

    public String[] toSortedArray() {
        ArrayList<String> result = new ArrayList<String>();
        inorder(root, result);
        return result.toArray(new String[0]);
    }

    private void inorder(Node node, ArrayList<String> result) {
        if (node == null) {
            return;
        }
        inorder(node.left, result);
        result.add(node.value);
        inorder(node.right, result);
    }

    public String delete(String value) {
        path.clear();
        successorPath.clear();
        successor = null;
        deleteType = "notfound";
        root = delete(root, value);
        if (!deleteType.equals("notfound")) {
            count--;
        }
        return deleteType;
    }

    private Node delete(Node node, String value) {
        if (node == null) {
            return null;
        }
        path.add(node.value);
        int result = compare(value, node.value);
        if (result < 0) {
            node.left = delete(node.left, value);
            return node;
        }
        if (result > 0) {
            node.right = delete(node.right, value);
            return node;
        }

        if (node.left == null && node.right == null) {
            deleteType = "leaf";
            return null;
        }
        if (node.left == null) {
            deleteType = "one";
            return node.right;
        }
        if (node.right == null) {
            deleteType = "one";
            return node.left;
        }

        deleteType = "two";
        Node smallest = node.right;
        successorPath.add(smallest.value);
        while (smallest.left != null) {
            smallest = smallest.left;
            successorPath.add(smallest.value);
        }
        successor = smallest.value;
        node.value = successor;
        node.right = removeSmallest(node.right);
        return node;
    }

    private Node removeSmallest(Node node) {
        if (node.left == null) {
            return node.right;
        }
        node.left = removeSmallest(node.left);
        return node;
    }

    public String getSuccessor() {
        return successor;
    }

    public String[] getSuccessorPath() {
        return successorPath.toArray(new String[0]);
    }

    public String[] getPath() {
        return path.toArray(new String[0]);
    }

    public Node getRoot() {
        return root;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public int size() {
        return count;
    }
}
