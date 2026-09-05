use aes_gcm::aead::{Aead, KeyInit, Payload};
use aes_gcm::{Aes256Gcm, Key, Nonce};
use base64::Engine;
use base64::engine::general_purpose::STANDARD;
use jni::JNIEnv;
use jni::objects::{JObject, JString};
use jni::sys::jstring;
use sha2::{Digest, Sha256};
use std::env;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use zeroize::Zeroize;

const CIPHERTEXT_PREFIX: &str = "v1:";
const NONCE_LEN: usize = 12;
static NONCE_COUNTER: AtomicU64 = AtomicU64::new(1);

/// Encrypts a secret with AES-256-GCM and returns a versioned ciphertext.
pub fn encrypt_secret(key_id: &str, plaintext: &str) -> Result<String, String> {
    if plaintext.trim().is_empty() {
        return Err("plaintext is empty".to_string());
    }
    let mut master = load_master_key(key_id)?;
    let mut key_bytes = derive_data_key(key_id, &master);
    master.zeroize();

    let cipher = Aes256Gcm::new(Key::<Aes256Gcm>::from_slice(&key_bytes));
    let nonce_bytes = derive_nonce(key_id, &master_nonce_seed(&key_bytes));
    let nonce = Nonce::from_slice(&nonce_bytes);
    let encrypted = cipher
        .encrypt(
            &nonce,
            Payload {
                msg: plaintext.as_bytes(),
                aad: normalized_key_id(key_id).as_bytes(),
            },
        )
        .map_err(|_| "encrypt failed".to_string())?;
    key_bytes.zeroize();

    let mut out = Vec::with_capacity(NONCE_LEN + encrypted.len());
    out.extend_from_slice(&nonce_bytes);
    out.extend_from_slice(&encrypted);
    Ok(format!("{}{}", CIPHERTEXT_PREFIX, STANDARD.encode(out)))
}

/// Decrypts a versioned AES-256-GCM ciphertext.
pub fn decrypt_secret(key_id: &str, ciphertext: &str) -> Result<String, String> {
    let raw = ciphertext.trim();
    if !raw.starts_with(CIPHERTEXT_PREFIX) {
        return Err("unsupported ciphertext format".to_string());
    }
    let encoded = &raw[CIPHERTEXT_PREFIX.len()..];
    let encrypted = STANDARD
        .decode(encoded)
        .map_err(|_| "ciphertext is not valid base64".to_string())?;
    if encrypted.len() <= NONCE_LEN {
        return Err("ciphertext is too short".to_string());
    }

    let (nonce_bytes, cipher_bytes) = encrypted.split_at(NONCE_LEN);
    let mut master = load_master_key(key_id)?;
    let mut key_bytes = derive_data_key(key_id, &master);
    master.zeroize();

    let cipher = Aes256Gcm::new(Key::<Aes256Gcm>::from_slice(&key_bytes));
    let decrypted = cipher
        .decrypt(
            Nonce::from_slice(nonce_bytes),
            Payload {
                msg: cipher_bytes,
                aad: normalized_key_id(key_id).as_bytes(),
            },
        )
        .map_err(|_| "decrypt failed".to_string())?;
    key_bytes.zeroize();

    String::from_utf8(decrypted).map_err(|_| "plaintext is not valid utf-8".to_string())
}

/// JNI bridge for util.payment.io.swzxsyh.NativeSecretJniClient.nativeResolve(String, String).
#[unsafe(no_mangle)]
pub extern "system" fn Java_io_swzxsyh_payment_util_NativeSecretJniClient_nativeResolve(
    mut env: JNIEnv,
    _this: JObject,
    key_id: JString,
    encrypted_value: JString,
) -> jstring {
    let key_id = match java_string(&mut env, &key_id) {
        Ok(value) => value,
        Err(message) => return throw_and_null(&mut env, message),
    };
    let encrypted_value = match java_string(&mut env, &encrypted_value) {
        Ok(value) => value,
        Err(message) => return throw_and_null(&mut env, message),
    };
    match decrypt_secret(&key_id, &encrypted_value) {
        Ok(value) => match env.new_string(value) {
            Ok(output) => output.into_raw(),
            Err(_) => throw_and_null(&mut env, "failed to allocate Java string".to_string()),
        },
        Err(message) => throw_and_null(&mut env, message),
    }
}

fn java_string(env: &mut JNIEnv, value: &JString) -> Result<String, String> {
    env.get_string(value)
        .map(|s| s.into())
        .map_err(|_| "failed to read Java string".to_string())
}

fn throw_and_null(env: &mut JNIEnv, message: String) -> jstring {
    let _ = env.throw_new("java/lang/IllegalStateException", message);
    std::ptr::null_mut()
}

fn load_master_key(key_id: &str) -> Result<Vec<u8>, String> {
    let specific_key = format!("CRYPTO_SECRET_JNI_KEY_{}", env_key_suffix(key_id));
    let raw = env::var(&specific_key)
        .or_else(|_| env::var("CRYPTO_SECRET_JNI_MASTER_KEY"))
        .map_err(|_| {
            format!(
                "missing env {} or CRYPTO_SECRET_JNI_MASTER_KEY",
                specific_key
            )
        })?;
    if raw.trim().is_empty() {
        return Err("master key is empty".to_string());
    }
    Ok(raw.into_bytes())
}

fn derive_data_key(key_id: &str, master_key: &[u8]) -> [u8; 32] {
    let mut hasher = Sha256::new();
    hasher.update(normalized_key_id(key_id).as_bytes());
    hasher.update([0]);
    hasher.update(master_key);
    hasher.finalize().into()
}

fn master_nonce_seed(key_bytes: &[u8; 32]) -> [u8; 32] {
    let mut hasher = Sha256::new();
    hasher.update(key_bytes);
    hasher.update(b":nonce");
    hasher.finalize().into()
}

fn derive_nonce(key_id: &str, seed: &[u8; 32]) -> [u8; NONCE_LEN] {
    let now_nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_nanos())
        .unwrap_or_default();
    let counter = NONCE_COUNTER.fetch_add(1, Ordering::Relaxed);
    let mut hasher = Sha256::new();
    hasher.update(seed);
    hasher.update(normalized_key_id(key_id).as_bytes());
    hasher.update(now_nanos.to_be_bytes());
    hasher.update(std::process::id().to_be_bytes());
    hasher.update(counter.to_be_bytes());
    let digest = hasher.finalize();
    let mut nonce = [0_u8; NONCE_LEN];
    nonce.copy_from_slice(&digest[..NONCE_LEN]);
    nonce
}

fn normalized_key_id(key_id: &str) -> String {
    key_id.trim().to_ascii_lowercase()
}

fn env_key_suffix(key_id: &str) -> String {
    key_id
        .trim()
        .chars()
        .map(|c| {
            if c.is_ascii_alphanumeric() {
                c.to_ascii_uppercase()
            } else {
                '_'
            }
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::{Mutex, OnceLock};

    static ENV_LOCK: OnceLock<Mutex<()>> = OnceLock::new();

    #[test]
    fn encrypt_then_decrypt() {
        let _guard = ENV_LOCK.get_or_init(|| Mutex::new(())).lock().unwrap();
        unsafe {
            env::set_var("CRYPTO_SECRET_JNI_MASTER_KEY", "test-master-key");
        }
        let plaintext = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        let encrypted = encrypt_secret("derived-hd-mnemonic", plaintext).unwrap();
        assert!(encrypted.starts_with(CIPHERTEXT_PREFIX));
        let decrypted = decrypt_secret("derived-hd-mnemonic", &encrypted).unwrap();
        assert_eq!(plaintext, decrypted);
    }
}
