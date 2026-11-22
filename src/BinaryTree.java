import java.util.ArrayList;

public class BinaryTree {
 
    public static class Node {
        public String value;
        public Node left;
        public Node right;

        public Node(String value) {
            this.value = value;
        }
    }

    private Node[] slots;
    private int count;
    private ArrayList<String> path = new ArrayList<String>();

    public BinaryTree(int capacity) {
        slots = new Node[capacity];
        count = 0;
    }

    public String add(String name) {
        path.clear();
        for (int i = 0; i < count; i++) {
            if (slots[i].value.equalsIgnoreCase(name)) {
                return "duplicate";
            }
        }
        if (count == slots.length) {
            return "full";
        }
        Node node = new Node(name);
        if (count > 0) {
            int parent = (count - 1) / 2;
            int p = parent;
            while (true) {
                path.add(0, slots[p].value);
                if (p == 0) {
                    break;
                }
                p = (p - 1) / 2;
            }
            if (count % 2 == 1) {
                slots[parent].left = node;
            } else {
                slots[parent].right = node;
            }
        }
        slots[count] = node;
        count++;
        return "ok";
    }

    public String[] preorder() {
        ArrayList<String> result = new ArrayList<String>();
        preorder(getRoot(), result);
        return result.toArray(new String[0]);
    }

    private void preorder(Node node, ArrayList<String> result) {
        if (node == null) {
            return;
        }
        result.add(node.value);
        preorder(node.left, result);
        preorder(node.right, result);
    }

    public String[] inorder() {
        ArrayList<String> result = new ArrayList<String>();
        inorder(getRoot(), result);
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

    public String[] postorder() {
        ArrayList<String> result = new ArrayList<String>();
        postorder(getRoot(), result);
        return result.toArray(new String[0]);
    }

    private void postorder(Node node, ArrayList<String> result) {
        if (node == null) {
            return;
        }
        postorder(node.left, result);
        postorder(node.right, result);
        result.add(node.value);
    }

    public String[] levelOrder() {
        ArrayList<String> result = new ArrayList<String>();
        ArrayList<Node> waiting = new ArrayList<Node>();
        if (getRoot() != null) {
            waiting.add(getRoot());
        }
        while (!waiting.isEmpty()) {
            Node node = waiting.remove(0);
            result.add(node.value);
            if (node.left != null) {
                waiting.add(node.left);
            }
            if (node.right != null) {
                waiting.add(node.right);
            }
        }
        return result.toArray(new String[0]);
    }

    public int height() {
        return height(getRoot());
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    public String[] getPath() {
        return path.toArray(new String[0]);
    }

    public Node getRoot() {
        if (count == 0) {
            return null;
        }
        return slots[0];
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
 