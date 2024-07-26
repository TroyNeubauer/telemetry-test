use rand::Rng;
use std::io::Write;
use std::net::TcpListener;
use std::time::Duration;

fn main() {
    let listener = TcpListener::bind("127.0.0.1:6969").unwrap();
    loop {
        let (mut client, addr) = listener.accept().unwrap();
        println!("Client {addr:?} connected");
        let mut byte = 0u8;
        let mut rng = rand::thread_rng();
        for i in 0..100_000 {
            let len = rng.gen_range(5..25);
            //let len = 4;
            println!("  msg[{i}].len = {len}");

            let payload: Vec<u8> = (0..len)
                .map(|_| {
                    byte = byte.wrapping_add(1);
                    byte
                })
                .collect();

            let mut p = build_packet(&payload);

            if rng.gen::<f64>() < 0.0001 {
                p.push(0);
                p.push(0);
                p.push(0);
                p.push(0);
            }
            println!("Sent: {p:?}");
            let _ = client.write_all(&p);
            let _ = client.flush();
            //std::thread::sleep(Duration::from_millis(50));
        }
        println!("Send done");
    }
}

fn build_packet(payload: &[u8]) -> Vec<u8> {
    let mut buf = vec![0u8; payload.len() + 3 + 2 + 2];
    buf[0..3].copy_from_slice(&[b'A', b'B', b'C']);

    let len = u16::to_le_bytes(payload.len().try_into().unwrap());
    buf[3..5].copy_from_slice(&len);
    buf[5..5 + payload.len()].copy_from_slice(payload);

    let crc = u16::to_le_bytes(calculate_crc(&buf[..5 + payload.len()]));
    buf[5 + payload.len()..5 + payload.len() + 2].copy_from_slice(&crc);

    buf
}

fn calculate_crc(bytes: &[u8]) -> u16 {
    let mut crc = 0x6969;
    for b in bytes {
        print!("{b} ");
        crc += *b as u16;
    }
    println!(" CRC complete: {crc}");

    crc
}
