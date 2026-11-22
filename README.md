New-Item -ItemType Directory -Force out
javac -d out src\*.java
java -cp out DSAVisualizerServer 

