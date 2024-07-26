use rand::Rng;
use std::io::Write;
use std::net::TcpListener;

fn main() {
    let listener = TcpListener::bind("127.0.0.1:6969").unwrap();
    loop {
        let (mut client, addr) = listener.accept().unwrap();
        println!("Client {addr:?} connected");
        let mut byte = 0u8;
        let mut rng = rand::thread_rng();
        for i in 0..100 {
            //let len = rng.gen_range(5..25);
            let len = (i % 6) + 4;
            //println!("  msg[{i}].len = {len}");

            let payload: Vec<u8> = (0..len)
                .map(|_| {
                    byte = byte.wrapping_add(1);
                    byte
                })
                .collect();

            let mut p = build_packet(&payload);

            if rng.gen::<f64>() < 0.01 {
                //println!("Appending inner bytes");
                let idx = rng.gen_range(0..p.len());
                //p.insert(idx, rng.gen());
            }

            if rng.gen::<f64>() < 0.1 {
                println!("Adding training bytes");
                let len = rng.gen_range(0..10);
                for _ in 0..len {
                    p.push(rng.gen());
                }
            }
            assert!(p.len() <= 32);
            //println!("Sent: {p:?}");
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
