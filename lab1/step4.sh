javac -d out *.java

# double buffering
java -cp out lab1.Benchmark double-buffer 1 5
java -cp out lab1.Benchmark double-buffer 2 5
java -cp out lab1.Benchmark double-buffer 4 5
java -cp out lab1.Benchmark double-buffer 8 5
java -cp out lab1.Benchmark double-buffer 16 5

# inconsistency tests
java -cp out lab1.InconsistencyTest double-buffer
java -cp out lab1.InconsistencyTest double-buffer-noverify
