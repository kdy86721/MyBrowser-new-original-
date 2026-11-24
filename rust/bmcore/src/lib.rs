//! `bmcore` — the native core of MyBrowser(BM).
//!
//! Pure, allocation-conscious implementations of the browser's hot-path algorithms (address-bar
//! resolution, per-request ad-block matching, security classification and string sanitizers).
//! They are exposed to Kotlin through JNI in [`jni_bridge`] and loaded as `libbmcore.so`;
//! `com.example.core.KotlinCoreAlgorithms` is the behavioural twin used when the library is absent.

pub mod ad_block;
pub mod sanitize;
pub mod url;

mod jni_bridge;

/// Bumped whenever a JNI signature changes; Kotlin refuses a mismatching library and falls back.
pub const ABI_VERSION: i32 = 1;
