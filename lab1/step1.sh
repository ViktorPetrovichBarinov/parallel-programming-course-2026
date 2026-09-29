javac -d out *.java

# all lock
java -cp out lab1.Benchmark sync 1 5
java -cp out lab1.Benchmark sync 2 5
java -cp out lab1.Benchmark sync 4 5
java -cp out lab1.Benchmark sync 8 5
java -cp out lab1.Benchmark sync 16 5

# empty-lock
java -cp out lab1.Benchmark empty-lock 1 5
java -cp out lab1.Benchmark empty-lock 2 5
java -cp out lab1.Benchmark empty-lock 4 5
java -cp out lab1.Benchmark empty-lock 8 5
java -cp out lab1.Benchmark empty-lock 16 5