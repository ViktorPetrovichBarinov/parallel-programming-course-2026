cd /home/russel/Repos/parallel-programming-course-2026/lab2/student-java

javac -d out src/lab2/Stress.java

java -cp out lab2.Stress 10000 none v1
java -cp out lab2.Stress 10000 yield v1
java -cp out lab2.Stress 10000 none v2