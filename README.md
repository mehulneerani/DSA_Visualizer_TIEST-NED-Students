# DSA Visualizer

Learn data structures by watching them work. DSA Visualizer ties each structure to an everyday story, so you can see what a program does when you add, remove or search.

| Story | Data structure |
|---|---|
| Print shop line | Queue |
| Library desk | Stack |
| Train coaches | Linked list |
| Family tree | Binary tree |
| Phone contacts | Binary search tree |

You set a size, type a name, pick an operation and watch the structure redraw. A small mascot explains each step in plain words, and a second panel shows the same data as it would look in real life.

## How it works

Every structure is written in plain Java, so each rule can be read in the code. The built-in Java HTTP server keeps one session per browser tab and returns each result as JSON. The page (HTML, CSS and JavaScript) only draws what the server sends, so it cannot show something the real structure would not allow.

## Run it

You need JDK 11 or newer. From the project folder:

    javac -d out src/*.java
    java -cp out DSAVisualizerServer

Then open http://localhost:8765 in your browser. Keep the terminal open while you use it.

## Authors

Built by **Mehul Kumar** (TCT-027) and **Tooba Ali** (TCT-001), who contributed equally.

CT-159: Data Structure Algorithms & Applications, taught by Engr. Anees Ahmed.
Department of Computer Science, Thar Institute of Engineering, Sciences & Technology.
