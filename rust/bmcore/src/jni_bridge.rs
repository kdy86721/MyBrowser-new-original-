//! JNI exports for `com.example.core.NativeBrowserCore` (Kotlin `object` + `@JvmStatic external`).
//!
//! Each export converts Java strings, runs the pure Rust function inside `catch_unwind`, and on
//! any failure returns a conservative fallback instead of unwinding into the JVM.

use crate::{ad_block, sanitize, url, ABI_VERSION};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::panic::{catch_unwind, AssertUnwindSafe};

/// Reads a Java string; `null` becomes an empty string.
fn read(env: &mut JNIEnv, s: &JString) -> String {
    if s.is_null() {
        return String::new();
    }
    env.get_string(s).map(Into::into).unwrap_or_default()
}

fn to_java(env: &mut JNIEnv, s: &str) -> jstring {
    env.new_string(s)
        .map(|j| j.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

/// Runs `f`, mapping a panic to `fallback`.
fn guarded<T>(fallback: T, f: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(f)).unwrap_or(fallback)
}

/// String -> String export with the input echoed back on panic.
fn map_string(env: &mut JNIEnv, input: &JString, f: impl FnOnce(&str) -> String) -> jstring {
    let s = read(env, input);
    let out = guarded(None, || Some(f(&s))).unwrap_or(s);
    to_java(env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_abiVersion(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    ABI_VERSION
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_normalizeOrSearch(
    mut env: JNIEnv,
    _class: JClass,
    raw_input: JString,
    home_url: JString,
    query_template: JString,
) -> jstring {
    let home = read(&mut env, &home_url);
    let template = read(&mut env, &query_template);
    map_string(&mut env, &raw_input, |raw| {
        url::normalize_or_search(raw, &home, &template)
    })
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_formatDisplayUrl(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    map_string(&mut env, &input, |u| url::format_display_url(u).to_owned())
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_classifySecurity(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jint {
    let s = read(&mut env, &input);
    guarded(url::SECURITY_NEUTRAL, || url::classify_security(&s))
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_isAdTrackerUrl(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jboolean {
    let s = read(&mut env, &input);
    if guarded(false, || ad_block::is_ad_tracker_url(&s)) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_sanitizeUserAgent(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    map_string(&mut env, &input, sanitize::sanitize_user_agent)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_sanitizeDownloadFileName(
    mut env: JNIEnv,
    _class: JClass,
    raw_name: JString,
    mime_type: JString,
    fallback_stem: JString,
) -> jstring {
    let mime = read(&mut env, &mime_type);
    let stem = read(&mut env, &fallback_stem);
    let fallback = format!("{stem}.bin");
    let raw = read(&mut env, &raw_name);
    let out = guarded(fallback, || {
        sanitize::sanitize_download_file_name(&raw, &mime, &stem)
    });
    to_java(&mut env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_sanitizeStateFileName(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    map_string(&mut env, &input, sanitize::sanitize_state_file_name)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_escapeJsSingleQuoted(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    map_string(&mut env, &input, sanitize::escape_js_single_quoted)
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_NativeBrowserCore_inferProviderSubtype(
    mut env: JNIEnv,
    _class: JClass,
    cli_type: JString,
    endpoint: JString,
) -> jstring {
    let cli = read(&mut env, &cli_type);
    let ep = read(&mut env, &endpoint);
    let out = guarded("", || sanitize::infer_provider_subtype(&cli, &ep));
    to_java(&mut env, out)
}
