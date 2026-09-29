javac -d out *.java

# striped
java -cp out lab1.Benchmark striped 1 5
java -cp out lab1.Benchmark striped 2 5
java -cp out lab1.Benchmark striped 4 5
java -cp out lab1.Benchmark striped 8 5
java -cp out lab1.Benchmark striped 16 5

# inconsistency test
java -cp out lab1.InconsistencyTest striped
