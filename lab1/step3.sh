javac -d out *.java

# thread-local
java -cp out lab1.Benchmark thread-local 1 5
java -cp out lab1.Benchmark thread-local 2 5
java -cp out lab1.Benchmark thread-local 4 5
java -cp out lab1.Benchmark thread-local 8 5
java -cp out lab1.Benchmark thread-local 16 5

# inconsistency test
java -cp out lab1.InconsistencyTest thread-local
