package lab1;

public record Snapshot(
    long[] buckets, //256 elements (deep copy)
    long count,
    long sum,
    long min,
    long max,
    long p50,
    long p99
) {}