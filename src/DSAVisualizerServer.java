import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DSAVisualizerServer {

    private static final int DEFAULT_PORT = 8765;
    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_BODY_BYTES = 4096;
    private static final int MAX_SESSIONS = 500;
    private static final Path PUBLIC_DIR = Paths.get("public").toAbsolutePath().normalize();

    private static final Map<String, String> KINDS = new HashMap<String, String>();
    static {
        KINDS.put("print", "queue");
        KINDS.put("library", "stack");
        KINDS.put("train", "list");
        KINDS.put("contacts", "tree");
        KINDS.put("family", "btree");
    }

    private static final String NEEDS_NAME = ",push,enq,first,last,at,find,ins,search,del,add,";

    private static final Map<String, Session> sessions =
            new LinkedHashMap<String, Session>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Session> eldest) {
                    return size() > MAX_SESSIONS;
                }
            };

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
            server.createContext("/api/", DSAVisualizerServer::handleApi);
            server.createContext("/", DSAVisualizerServer::handleStatic);
            server.start();
            System.out.println("DSA Visualizer is running. Open http://localhost:" + port);
            System.out.println("Press Ctrl+C to stop.");
        } catch (BindException e) {
            System.err.println("Port " + port + " is already in use. Try another: java -cp out DSAVisualizerServer 8766");
            System.exit(1);
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "text/plain; charset=utf-8", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String urlPath = ex.getRequestURI().getPath();
            if (urlPath.equals("/")) {
                urlPath = "/index.html";
            }
            Path file;
            try {
                file = PUBLIC_DIR.resolve(urlPath.substring(1)).normalize();
            } catch (RuntimeException badPath) {
                file = null;
            }
            if (file == null || !file.startsWith(PUBLIC_DIR) || !Files.isRegularFile(file)) {
                send(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            send(ex, 200, contentType(file.getFileName().toString()), Files.readAllBytes(file));
        } finally {
            ex.close();
        }
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "text/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        int status = 200;
        String body;
        try {
            if (!"POST".equals(ex.getRequestMethod())) {
                throw new ApiException(405, "Use POST.");
            }
            String action = ex.getRequestURI().getPath().substring("/api/".length());
            Map<String, String> params = parseForm(readBody(ex.getRequestBody()));
            body = process(action, params);
        } catch (ApiException e) {
            status = e.status;
            body = new Json().bool("ok", false).str("error", e.getMessage()).end();
        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            body = new Json().bool("ok", false).str("error", "Something went wrong on the server.").end();
        }
        try {
            send(ex, status, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
        } finally {
            ex.close();
        }
    }

    private static synchronized String process(String action, Map<String, String> p) {
        String sid = p.get("sid");
        if (sid == null || !sid.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new ApiException(400, "Missing or invalid session id.");
        }
        String scenario = p.get("scenario");
        String kind = (scenario == null) ? null : KINDS.get(scenario);
        if (kind == null) {
            throw new ApiException(400, "Unknown story.");
        }
        String key = sid + "|" + scenario;
        Session session = sessions.get(key);

        switch (action) {
            case "reset": {
                session = new Session(scenario, kind);
                sessions.put(key, session);
                return session.toJson(null);
            }
            case "size": {
                int size;
                try {
                    size = Integer.parseInt(p.get("size"));
                } catch (NumberFormatException e) {
                    throw new ApiException(400, "Please enter a number from 1 to 10.");
                }
                if (size < 1 || size > 10) {
                    throw new ApiException(400, "Please enter a number from 1 to 10.");
                }
                if (session == null) {
                    session = new Session(scenario, kind);
                    sessions.put(key, session);
                }
                session.setCapacity(size);
                return session.toJson(null);
            }
            case "op": {
                if (session == null || session.capacity == 0) {
                    throw new ApiException(409, "Set the size first, then try an operation.");
                }
                String op = p.get("op") == null ? "" : p.get("op");
                String name = p.get("value") == null ? "" : p.get("value").trim();
                if (NEEDS_NAME.contains("," + op + ",")) {
                    if (name.isEmpty()) {
                        throw new ApiException(400, "Type a name first, then press the operation.");
                    }
                    if (name.length() > MAX_NAME_LENGTH) {
                        throw new ApiException(400, "Names can have at most " + MAX_NAME_LENGTH + " characters.");
                    }
                }
                return session.run(op, name, parsePosition(p.get("pos")));
            }
            default:
                throw new ApiException(404, "Unknown action.");
        }
    }

    private static int parsePosition(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static class Session {
        final String scenario;
        final String kind;
        int capacity = 0;

        Stack stack;
        Queue queue;
        SinglyLinkedList list;
        BinarySearchTree tree;
        BinaryTree btree;

        final ArrayList<String> served = new ArrayList<String>();
        final ArrayList<String> returned = new ArrayList<String>();

        Session(String scenario, String kind) {
            this.scenario = scenario;
            this.kind = kind;
        }

        void setCapacity(int n) {
            capacity = n;
            stack = null; queue = null; list = null; tree = null; btree = null;
            served.clear();
            returned.clear();
            switch (kind) {
                case "stack": stack = new Stack(n); break;
                case "queue": queue = new Queue(n); break;
                case "list":  list = new SinglyLinkedList(n); break;
                case "tree":  tree = new BinarySearchTree(n); break;
                default:      btree = new BinaryTree(n); break;
            }
        }

        String run(String op, String name, int pos) {
            Json out = new Json().bool("ok", true).str("op", op);
            if (!name.isEmpty()) {
                out.str("value", name);
            }
            switch (kind) {
                case "stack": runStack(op, name, out); break;
                case "queue": runQueue(op, name, out); break;
                case "list":  runList(op, name, pos, out);  break;
                case "tree":  runTree(op, name, out);  break;
                default:      runBinaryTree(op, name, out); break;
            }
            return toJson(out);
        }

        private void runStack(String op, String name, Json out) {
            switch (op) {
                case "push": {
                    boolean ok = stack.push(name);
                    out.str("status", ok ? "ok" : "full");
                    if (ok) {
                        out.str("x", name);
                        returned.remove(name);
                    }
                    break;
                }
                case "pop": {
                    String x = stack.pop();
                    out.str("status", x == null ? "underflow" : "ok");
                    if (x != null) {
                        out.str("x", x);
                        returned.add(x);
                    }
                    break;
                }
                case "peek": {
                    String x = stack.peek();
                    out.str("status", x == null ? "underflow" : "ok");
                    if (x != null) {
                        out.str("x", x);
                    }
                    break;
                }
                case "empty":
                    out.str("status", "ok").bool("empty", stack.isEmpty());
                    break;
                case "full":
                    out.str("status", "ok").bool("full", stack.isFull());
                    break;
                default:
                    throw new ApiException(400, "Unknown operation.");
            }
        }

        private void runQueue(String op, String name, Json out) {
            switch (op) {
                case "enq": {
                    boolean ok = queue.enqueue(name);
                    out.str("status", ok ? "ok" : "full");
                    if (ok) {
                        out.str("x", name);
                    }
                    break;
                }
                case "deq": {
                    String x = queue.dequeue();
                    out.str("status", x == null ? "underflow" : "ok");
                    if (x != null) {
                        out.str("x", x);
                        served.add(x);
                    }
                    break;
                }
                case "front": {
                    String x = queue.front();
                    out.str("status", x == null ? "underflow" : "ok");
                    if (x != null) {
                        out.str("x", x);
                    }
                    break;
                }
                case "empty":
                    out.str("status", "ok").bool("empty", queue.isEmpty());
                    break;
                default:
                    throw new ApiException(400, "Unknown operation.");
            }
        }

        private void runList(String op, String name, int pos, Json out) {
            switch (op) {
                case "first": {
                    boolean ok = list.addFirst(name);
                    out.str("status", ok ? "ok" : "full");
                    if (ok) {
                        out.str("x", name);
                    }
                    break;
                }
                case "last": {
                    boolean ok = list.addLast(name);
                    out.str("status", ok ? "ok" : "full");
                    if (ok) {
                        out.str("x", name);
                    }
                    break;
                }
                case "at": {
                    boolean full = list.isFull();
                    boolean ok = list.addAt(name, pos);
                    out.str("status", ok ? "ok" : full ? "full" : "badpos")
                       .num("pos", pos)
                       .num("max", list.size() + 1);
                    if (ok) {
                        out.str("x", name);
                    }
                    break;
                }
                case "rem": {
                    String x = list.removeFirst();
                    out.str("status", x == null ? "underflow" : "ok");
                    if (x != null) {
                        out.str("x", x);
                    }
                    break;
                }
                case "del": {
                    boolean wasEmpty = list.isEmpty();
                    int index = list.remove(name);
                    out.str("status", index >= 0 ? "ok" : "notfound")
                       .bool("wasEmpty", wasEmpty)
                       .num("index", index);
                    if (index >= 0) {
                        out.str("x", name);
                    }
                    break;
                }
                case "find": {
                    boolean wasEmpty = list.isEmpty();
                    int index = list.find(name);
                    out.str("status", index >= 0 ? "found" : "notfound")
                       .bool("wasEmpty", wasEmpty)
                       .num("index", index);
                    break;
                }
                default:
                    throw new ApiException(400, "Unknown operation.");
            }
        }

        private void runTree(String op, String name, Json out) {
            switch (op) {
                case "ins": {
                    String status = tree.insert(name);
                    out.str("status", status).arr("path", tree.getPath());
                    break;
                }
                case "search": {
                    boolean wasEmpty = tree.isEmpty();
                    boolean hit = tree.search(name);
                    out.str("status", hit ? "found" : "notfound")
                       .bool("wasEmpty", wasEmpty)
                       .arr("path", tree.getPath());
                    break;
                }
                case "min": {
                    String x = tree.findMin();
                    out.str("status", x == null ? "underflow" : "ok")
                       .arr("path", tree.getPath());
                    if (x != null) {
                        out.str("x", x);
                    }
                    break;
                }
                case "order": {
                    String[] order = tree.toSortedArray();
                    out.str("status", order.length == 0 ? "underflow" : "ok")
                       .arr("order", order);
                    break;
                }
                case "del": {
                    boolean wasEmpty = tree.isEmpty();
                    String result = tree.delete(name);
                    out.bool("wasEmpty", wasEmpty)
                       .str("status", result.equals("notfound") ? "notfound" : "ok")
                       .arr("path", tree.getPath());
                    if (!result.equals("notfound")) {
                        out.str("case", result);
                    }
                    if (result.equals("two")) {
                        out.str("succ", tree.getSuccessor()).arr("spath", tree.getSuccessorPath());
                    }
                    break;
                }
                default:
                    throw new ApiException(400, "Unknown operation.");
            }
        }

        private void runBinaryTree(String op, String name, Json out) {
            switch (op) {
                case "add": {
                    String status = btree.add(name);
                    out.str("status", status)
                       .arr("path", btree.getPath());
                    break;
                }
                case "pre":
                case "in":
                case "post":
                case "level": {
                    String[] order = op.equals("pre") ? btree.preorder()
                                   : op.equals("in") ? btree.inorder()
                                   : op.equals("post") ? btree.postorder() : btree.levelOrder();
                    out.str("status", order.length == 0 ? "underflow" : "ok")
                       .arr("order", order);
                    break;
                }
                case "height":
                    out.str("status", btree.isEmpty() ? "underflow" : "ok")
                       .num("height", btree.height());
                    break;
                default:
                    throw new ApiException(400, "Unknown operation.");
            }
        }

        String toJson(Json out) {
            if (out == null) {
                out = new Json().bool("ok", true);
            }
            String[] items = new String[0];
            int size = 0;
            if (stack != null) {
                items = stack.toArray();
                size = stack.size();
            } else if (queue != null) {
                items = queue.toArray();
                size = queue.size();
            } else if (list != null) {
                items = list.toArray();
                size = list.size();
            } else if (tree != null) {
                size = tree.size();
            } else if (btree != null) {
                size = btree.size();
            }
            out.str("scenario", scenario).num("capacity", capacity).num("size", size);
            out.arr("items", items);
            if (tree != null) {
                out.raw("nodes", treeNodesJson(tree)).arr("sorted", tree.toSortedArray());
            } else if (btree != null) {
                out.raw("nodes", btreeNodesJson(btree)).arr("sorted", new String[0]);
            } else {
                out.raw("nodes", "[]").arr("sorted", new String[0]);
            }
            out.arr("served", served.toArray(new String[0]));
            out.arr("returned", returned.toArray(new String[0]));
            return out.end();
        }
    }

    private static String treeNodesJson(BinarySearchTree tree) {
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> depths = new ArrayList<Integer>();
        ArrayList<Integer> lefts = new ArrayList<Integer>();
        ArrayList<Integer> rights = new ArrayList<Integer>();
        flatten(tree.getRoot(), 0, names, depths, lefts, rights);
        return nodesToJson(names, depths, lefts, rights);
    }

    private static int flatten(BinarySearchTree.Node node, int depth,
        ArrayList<String> names, ArrayList<Integer> depths,
        ArrayList<Integer> lefts, ArrayList<Integer> rights) {
        if (node == null) {
            return -1;
        }
        int left = flatten(node.left, depth + 1, names, depths, lefts, rights);
        int me = names.size();
        names.add(node.value);
        depths.add(depth);
        lefts.add(left);
        rights.add(-1);
        int right = flatten(node.right, depth + 1, names, depths, lefts, rights);
        rights.set(me, right);
        return me;
    }

    private static String btreeNodesJson(BinaryTree tree) {
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> depths = new ArrayList<Integer>();
        ArrayList<Integer> lefts = new ArrayList<Integer>();
        ArrayList<Integer> rights = new ArrayList<Integer>();
        flattenPlain(tree.getRoot(), 0, names, depths, lefts, rights);
        return nodesToJson(names, depths, lefts, rights);
    }

    private static int flattenPlain(BinaryTree.Node node, int depth,
        ArrayList<String> names, ArrayList<Integer> depths,
        ArrayList<Integer> lefts, ArrayList<Integer> rights) {
        if (node == null) {
            return -1;
        }
        int left = flattenPlain(node.left, depth + 1, names, depths, lefts, rights);
        int me = names.size();
        names.add(node.value);
        depths.add(depth);
        lefts.add(left);
        rights.add(-1);
        int right = flattenPlain(node.right, depth + 1, names, depths, lefts, rights);
        rights.set(me, right);
        return me;
    }

    private static String nodesToJson(ArrayList<String> names, ArrayList<Integer> depths,
        ArrayList<Integer> lefts, ArrayList<Integer> rights) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"v\":").append(Json.quote(names.get(i)))
              .append(",\"x\":").append(i)
              .append(",\"d\":").append(depths.get(i))
              .append(",\"l\":").append(lefts.get(i))
              .append(",\"r\":").append(rights.get(i)).append('}');
        }
        return sb.append(']').toString();
    }

    private static class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int total = 0;
        int n;
        while ((n = in.read(chunk)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) {
                throw new ApiException(413, "Request is too large.");
            }
            buffer.write(chunk, 0, n);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        if (body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            map.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
        }
        return map;
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, body.length);
        OutputStream os = ex.getResponseBody();
        try {
            os.write(body);
        } finally {
            os.close();
        }
    }

    private static class Json {
        private final StringBuilder sb = new StringBuilder("{");
        private boolean first = true;

        private Json key(String k) {
            if (!first) sb.append(',');
            first = false;
            sb.append(quote(k)).append(':');
            return this;
        }

        Json str(String k, String v) { key(k); sb.append(v == null ? "null" : quote(v)); return this; }
        Json num(String k, int v) { key(k); sb.append(v); return this; }
        Json bool(String k, boolean v) { key(k); sb.append(v); return this; }
        Json raw(String k, String json) { key(k); sb.append(json); return this; }

        Json arr(String k, String[] values) {
            key(k);
            sb.append('[');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(quote(values[i]));
            }
            sb.append(']');
            return this;
        }

        String end() { return sb.toString() + "}"; }

        static String quote(String s) {
            StringBuilder q = new StringBuilder("\"");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"':  q.append("\\\""); break;
                    case '\\': q.append("\\\\"); break;
                    case '\n': q.append("\\n");  break;
                    case '\r': q.append("\\r");  break;
                    case '\t': q.append("\\t");  break;
                    default:
                        if (c < 0x20) {
                            q.append(String.format("\\u%04x", (int) c));
                        } else {
                            q.append(c);
                        }
                }
            }
            return q.append('"').toString();
        }
    }
}
