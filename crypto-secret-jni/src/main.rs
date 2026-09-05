use crypto_secret_jni::{decrypt_secret, encrypt_secret};
use std::env;
use std::io::{self, Read};
use std::process;

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 3 {
        usage_and_exit();
    }

    let command = args[1].as_str();
    let key_id = args[2].as_str();
    let input = if args.len() >= 4 {
        args[3..].join(" ")
    } else {
        read_stdin()
    };

    let result = match command {
        "encrypt" => encrypt_secret(key_id, input.trim_end()),
        "decrypt" => decrypt_secret(key_id, input.trim_end()),
        _ => {
            usage_and_exit();
        }
    };

    match result {
        Ok(value) => println!("{value}"),
        Err(message) => {
            eprintln!("{message}");
            process::exit(2);
        }
    }
}

fn read_stdin() -> String {
    let mut input = String::new();
    if let Err(error) = io::stdin().read_to_string(&mut input) {
        eprintln!("failed to read stdin: {error}");
        process::exit(2);
    }
    input
}

fn usage_and_exit() -> ! {
    eprintln!("Usage:");
    eprintln!("  crypto-secret-tool encrypt <keyId> <plaintext>");
    eprintln!("  crypto-secret-tool decrypt <keyId> <ciphertext>");
    eprintln!();
    eprintln!("Required env:");
    eprintln!("  CRYPTO_SECRET_JNI_MASTER_KEY=<master-key>");
    eprintln!("  or CRYPTO_SECRET_JNI_KEY_<KEY_ID>=<master-key>");
    process::exit(1);
}
