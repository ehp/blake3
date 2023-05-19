# Blake3 Hashing Library

A simple hashing library that implements Blake3 hash function in pure Java.
Based on [reference implementation](https://github.com/BLAKE3-team/BLAKE3) with critical part inspired by [Scala library](https://github.com/catap/scala-blake3). 

## Build

```bash
./gradlew build
```

## Usage

### Regular hash

```java
final var hasher = new Hasher();
hasher.update(inputBytes);
final var hash = hasher.finalizeHash();
```

### Keyed hash

```java
final var key = new byte[] {...}; 
final var hasher = new Hasher(key);
hasher.update(inputBytes);
final var hash = hasher.finalizeHash();
```

### Derivation hash

```java
final var content = "...";
final var hasher = new Hasher(content);
hasher.update(inputBytes);
final var hash = hasher.finalizeHash(expected.length);
```

## TODO

- JCE provider
- SIMD vectorized implementation
- Benchmarks

## License

This library is licensed under the Apache 2.0 License.
