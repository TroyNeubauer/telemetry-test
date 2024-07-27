
# telemetry-test

Kotlin variable payload size telemetry streaming for unreliable networks. Features:
* CRC check
* Scanning for next magic when corruption is detected
* Invariant to read sizes from kernel (properly handles a packet being split into partial reads)
* No OSI level 3 dependence (TCP or UDP or any other)
* Rust based fuzzing tester

Based loosely on tokio's [ReadBuf](https://docs.rs/tokio/latest/tokio/io/struct.ReadBuf.html)

