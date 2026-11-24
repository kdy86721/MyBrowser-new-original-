//! URL resolution, display formatting and security classification.
//!
//! Behaviour mirrors `KotlinCoreAlgorithms` on the Kotlin side (the fallback used when the native
//! library is unavailable, e.g. in JVM unit tests). Keep both in sync.

/// Ordinals of `com.example.core.SecurityState` (SECURE, INSECURE, NEUTRAL).
pub const SECURITY_SECURE: i32 = 0;
pub const SECURITY_INSECURE: i32 = 1;
pub const SECURITY_NEUTRAL: i32 = 2;

/// Kotlin/JVM `Char.isWhitespace()`: `Character.isWhitespace || Character.isSpaceChar`, i.e.
/// Unicode Zs/Zl/Zp plus `\t \n \u{0B} \u{0C} \r` and `\u{1C}..=\u{1F}` (but not U+0085, unlike
/// Rust's `char::is_whitespace`).
#[inline]
pub fn is_kotlin_whitespace(c: char) -> bool {
    matches!(
        c,
        '\t' | '\n' | '\u{0B}' | '\u{0C}' | '\r' | '\u{1C}'..='\u{1F}' | ' ' | '\u{A0}' | '\u{1680}'
            | '\u{2000}'..='\u{200A}' | '\u{2028}' | '\u{2029}' | '\u{202F}' | '\u{205F}' | '\u{3000}'
    )
}

/// Kotlin `String.trim()`.
#[inline]
pub fn kt_trim(s: &str) -> &str {
    s.trim_matches(is_kotlin_whitespace)
}

/// ASCII case-insensitive `starts_with`. All prefixes used here are ASCII, so slicing
/// `s[prefix.len()..]` after a match is always on a char boundary.
#[inline]
pub(crate) fn starts_with_ignore_ascii_case(s: &str, prefix: &str) -> bool {
    let (s, p) = (s.as_bytes(), prefix.as_bytes());
    s.len() >= p.len() && s[..p.len()].eq_ignore_ascii_case(p)
}

/// Resolves address-bar input into a loadable URL, or a search URL built from `query_template`
/// (`%s` is replaced with the form-encoded query). Empty input resolves to `home_url`.
pub fn normalize_or_search(raw_input: &str, home_url: &str, query_template: &str) -> String {
    let trimmed = kt_trim(raw_input);
    if trimmed.is_empty() {
        return home_url.to_owned();
    }

    if starts_with_ignore_ascii_case(trimmed, "https://") {
        return concat("https://", &trimmed[8..]);
    }
    if starts_with_ignore_ascii_case(trimmed, "http://") {
        return concat("http://", &trimmed[7..]);
    }
    if starts_with_ignore_ascii_case(trimmed, "about:")
        || starts_with_ignore_ascii_case(trimmed, "file://")
        || starts_with_ignore_ascii_case(trimmed, "data:")
    {
        return trimmed.to_owned();
    }

    if !trimmed.chars().any(is_kotlin_whitespace) {
        let host_part = before(before(trimmed, '/'), '?');
        let (host, port) = match host_part.find(':') {
            Some(i) => (&host_part[..i], &host_part[i + 1..]),
            None => (host_part, ""),
        };
        let valid_port = port.bytes().all(|b| b.is_ascii_digit());
        if valid_port && is_local_or_ip_host(host) {
            return concat("http://", trimmed);
        }
        if valid_port && looks_like_domain(host) {
            return concat("https://", trimmed);
        }
    }

    query_template.replace("%s", &percent_encode_form(trimmed))
}

/// Strips scheme, a leading `www.` and trailing slashes for compact address-bar display.
pub fn format_display_url(url: &str) -> &str {
    let trimmed = kt_trim(url);
    if trimmed.is_empty() {
        return "";
    }
    let without_scheme = if starts_with_ignore_ascii_case(trimmed, "https://") {
        &trimmed[8..]
    } else if starts_with_ignore_ascii_case(trimmed, "http://") {
        &trimmed[7..]
    } else {
        trimmed
    };
    let without_www = if starts_with_ignore_ascii_case(without_scheme, "www.") {
        &without_scheme[4..]
    } else {
        without_scheme
    };
    without_www.trim_end_matches('/')
}

pub fn classify_security(url: &str) -> i32 {
    let trimmed = kt_trim(url);
    if starts_with_ignore_ascii_case(trimmed, "https://") {
        SECURITY_SECURE
    } else if starts_with_ignore_ascii_case(trimmed, "http://") {
        SECURITY_INSECURE
    } else {
        SECURITY_NEUTRAL
    }
}

pub fn is_local_or_ip_host(host: &str) -> bool {
    if host.eq_ignore_ascii_case("localhost") {
        return true;
    }
    let mut count = 0;
    for part in host.split('.') {
        count += 1;
        if count > 4 {
            return false;
        }
        // Same acceptance as Kotlin's `toIntOrNull()` (optional sign, decimal digits).
        match part.parse::<i64>() {
            Ok(n) if (0..=255).contains(&n) => {}
            _ => return false,
        }
    }
    count == 4
}

pub fn looks_like_domain(host: &str) -> bool {
    if host.chars().count() < 3 || !host.contains('.') {
        return false;
    }
    if host.starts_with('.') || host.ends_with('.') {
        return false;
    }
    let tld = host.rsplit('.').next().unwrap_or("");
    if tld.chars().count() < 2 || !tld.chars().all(char::is_alphabetic) {
        return false;
    }
    host.split('.').all(|label| {
        !label.is_empty()
            && !label.starts_with('-')
            && !label.ends_with('-')
            && label.chars().all(|c| c.is_alphanumeric() || c == '-')
    })
}

/// `application/x-www-form-urlencoded` style UTF-8 percent encoding (space -> `+`).
pub fn percent_encode_form(input: &str) -> String {
    const HEX: &[u8; 16] = b"0123456789ABCDEF";
    let mut out = String::with_capacity(input.len() * 3);
    for &b in input.as_bytes() {
        match b {
            b'a'..=b'z' | b'A'..=b'Z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(b as char)
            }
            b' ' => out.push('+'),
            _ => {
                out.push('%');
                out.push(HEX[(b >> 4) as usize] as char);
                out.push(HEX[(b & 0x0F) as usize] as char);
            }
        }
    }
    out
}

/// Substring before the first `delim` (whole string when absent), like Kotlin's `substringBefore`.
#[inline]
pub(crate) fn before(s: &str, delim: char) -> &str {
    s.find(delim).map_or(s, |i| &s[..i])
}

#[inline]
fn concat(a: &str, b: &str) -> String {
    let mut s = String::with_capacity(a.len() + b.len());
    s.push_str(a);
    s.push_str(b);
    s
}

#[cfg(test)]
mod tests {
    use super::*;

    const DDG: &str = "https://duckduckgo.com/?q=%s";
    const HOME: &str = "https://duckduckgo.com";

    #[test]
    fn resolves_domains_local_hosts_and_queries() {
        assert_eq!(
            normalize_or_search("example.com", HOME, DDG),
            "https://example.com"
        );
        assert_eq!(
            normalize_or_search("localhost:8080/api", HOME, DDG),
            "http://localhost:8080/api"
        );
        assert_eq!(
            normalize_or_search("192.168.1.1", HOME, DDG),
            "http://192.168.1.1"
        );
        assert_eq!(
            normalize_or_search("rust ffi android", HOME, DDG),
            "https://duckduckgo.com/?q=rust+ffi+android"
        );
        assert_eq!(
            normalize_or_search("kotlin", HOME, "https://search.brave.com/search?q=%s"),
            "https://search.brave.com/search?q=kotlin"
        );
        assert_eq!(normalize_or_search("   ", HOME, DDG), HOME);
    }

    #[test]
    fn keeps_explicit_schemes_and_lowercases_scheme_only() {
        assert_eq!(
            normalize_or_search("HTTPS://Example.com/A", HOME, DDG),
            "https://Example.com/A"
        );
        assert_eq!(normalize_or_search("Http://x.io", HOME, DDG), "http://x.io");
        assert_eq!(normalize_or_search("about:blank", HOME, DDG), "about:blank");
        assert_eq!(
            normalize_or_search("data:text/plain,hi", HOME, DDG),
            "data:text/plain,hi"
        );
    }

    #[test]
    fn rejects_non_domains_and_encodes_utf8() {
        assert_eq!(
            normalize_or_search("a.1", HOME, DDG),
            "https://duckduckgo.com/?q=a.1"
        );
        assert_eq!(
            normalize_or_search("-bad.com", HOME, DDG),
            "https://duckduckgo.com/?q=-bad.com"
        );
        assert_eq!(
            normalize_or_search("example.com:abc", HOME, DDG),
            "https://duckduckgo.com/?q=example.com%3Aabc"
        );
        assert_eq!(
            normalize_or_search("256.1.1.1", HOME, DDG),
            "https://duckduckgo.com/?q=256.1.1.1"
        );
        assert_eq!(
            normalize_or_search("러스트 jni", HOME, DDG),
            "https://duckduckgo.com/?q=%EB%9F%AC%EC%8A%A4%ED%8A%B8+jni"
        );
        assert_eq!(
            normalize_or_search("네이버.한국", HOME, DDG),
            "https://네이버.한국"
        );
    }

    #[test]
    fn trims_like_kotlin() {
        assert_eq!(kt_trim("\u{1C}\u{3000} a \u{2028}"), "a");
        assert_eq!(kt_trim("\u{85}a"), "\u{85}a");
        assert_eq!(
            normalize_or_search("\u{A0}example.com\u{1F}", HOME, DDG),
            "https://example.com"
        );
    }

    #[test]
    fn formats_display_urls() {
        assert_eq!(
            format_display_url("https://www.example.com/"),
            "example.com"
        );
        assert_eq!(
            format_display_url("HTTP://WWW.Example.com/path//"),
            "Example.com/path"
        );
        assert_eq!(format_display_url("about:blank"), "about:blank");
        assert_eq!(format_display_url("  "), "");
    }

    #[test]
    fn classifies_security() {
        assert_eq!(classify_security(" HTTPS://a.b"), SECURITY_SECURE);
        assert_eq!(classify_security("http://a.b"), SECURITY_INSECURE);
        assert_eq!(classify_security("about:blank"), SECURITY_NEUTRAL);
    }
}
