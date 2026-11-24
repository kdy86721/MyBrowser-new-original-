//! String sanitizers shared by the WebView, download and storage layers, plus AI provider
//! inference. Hand-written scanners replace the Kotlin regexes, so no regex engine is shipped.

use crate::url::{before, kt_trim, starts_with_ignore_ascii_case};

/// `java.util.regex` `\s` (default, non-Unicode mode): `[ \t\n\x0B\f\r]`.
#[inline]
fn is_java_space(c: char) -> bool {
    matches!(c, ' ' | '\t' | '\n' | '\u{0B}' | '\u{0C}' | '\r')
}

/// ASCII word char, i.e. the `(?<![A-Za-z0-9_])` boundary used by the Kotlin fallback.
#[inline]
fn is_word_char(c: char) -> bool {
    c.is_ascii_alphanumeric() || c == '_'
}

/// Strips WebView-only User-Agent tokens (`; wv` and `Version/x.y`) so OAuth providers treat
/// the browser as regular mobile Chrome. Equivalent to the previous regex chain:
/// `;\s*wv(?=\))` -> "", `(?<![A-Za-z0-9_])Version/[0-9]+\.[0-9]+\s*` -> "",
/// `\s{2,}` -> " " (ASCII `\s`), then trim.
pub fn sanitize_user_agent(raw: &str) -> String {
    kt_trim(&collapse_spaces(&remove_version_token(&remove_wv_token(
        raw,
    ))))
    .to_owned()
}

fn remove_wv_token(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    let mut i = 0;
    while let Some(off) = s[i..].find(';') {
        let semi = i + off;
        out.push_str(&s[i..semi]);
        let after = &s[semi + 1..];
        let ws_len: usize = after
            .chars()
            .take_while(|&c| is_java_space(c))
            .map(char::len_utf8)
            .sum();
        if after[ws_len..].starts_with("wv)") {
            // Drop `;<spaces>wv`, keep the `)` (it was only a lookahead).
            i = semi + 1 + ws_len + 2;
        } else {
            out.push(';');
            i = semi + 1;
        }
    }
    out.push_str(&s[i..]);
    out
}

fn remove_version_token(s: &str) -> String {
    const TOKEN: &str = "Version/";
    let mut out = String::with_capacity(s.len());
    let mut i = 0;
    while let Some(off) = s[i..].find(TOKEN) {
        let start = i + off;
        let boundary = s[..start]
            .chars()
            .next_back()
            .is_none_or(|c| !is_word_char(c));
        let tail = &s.as_bytes()[start + TOKEN.len()..];
        let major = tail.iter().take_while(|b| b.is_ascii_digit()).count();
        let minor = if major > 0 && tail.get(major) == Some(&b'.') {
            tail[major + 1..]
                .iter()
                .take_while(|b| b.is_ascii_digit())
                .count()
        } else {
            0
        };
        if boundary && minor > 0 {
            let mut end = start + TOKEN.len() + major + 1 + minor;
            end += s[end..]
                .chars()
                .take_while(|&c| is_java_space(c))
                .map(char::len_utf8)
                .sum::<usize>();
            out.push_str(&s[i..start]);
            i = end;
        } else {
            out.push_str(&s[i..start + 1]);
            i = start + 1;
        }
    }
    out.push_str(&s[i..]);
    out
}

fn collapse_spaces(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    let mut run = String::new();
    for c in s.chars() {
        if is_java_space(c) {
            run.push(c);
            continue;
        }
        flush_space_run(&mut out, &mut run);
        out.push(c);
    }
    flush_space_run(&mut out, &mut run);
    out
}

#[inline]
fn flush_space_run(out: &mut String, run: &mut String) {
    match run.chars().count() {
        0 => {}
        1 => out.push_str(run),
        _ => out.push(' '),
    }
    run.clear();
}

/// Makes a downloaded file name safe for MediaStore / DownloadManager. When the name is unusable,
/// `fallback_stem` plus an extension derived from `mime_type` is returned.
pub fn sanitize_download_file_name(raw_name: &str, mime_type: &str, fallback_stem: &str) -> String {
    let cleaned: String = kt_trim(raw_name)
        .chars()
        .map(|c| match c {
            '\\' | '/' | ':' | '*' | '?' | '"' | '<' | '>' | '|' => '_',
            other => other,
        })
        .collect();
    if !kt_trim(&cleaned).is_empty() && cleaned != "downloadfile.bin" {
        return cleaned;
    }
    let mime = mime_type.to_lowercase();
    let ext = match kt_trim(before(&mime, ';')) {
        "image/png" => "png",
        "image/jpeg" | "image/jpg" => "jpg",
        "image/webp" => "webp",
        "image/gif" => "gif",
        "application/pdf" => "pdf",
        "text/plain" => "txt",
        "text/csv" => "csv",
        "application/json" => "json",
        "application/zip" => "zip",
        "video/mp4" => "mp4",
        "audio/mpeg" => "mp3",
        _ => "bin",
    };
    let mut out = String::with_capacity(fallback_stem.len() + 1 + ext.len());
    out.push_str(fallback_stem);
    out.push('.');
    out.push_str(ext);
    out
}

/// Maps every char outside `[A-Za-z0-9._-]` to `_` (per-tab WebView state file names).
pub fn sanitize_state_file_name(raw: &str) -> String {
    raw.chars()
        .map(|c| {
            if c.is_ascii_alphanumeric() || matches!(c, '.' | '_' | '-') {
                c
            } else {
                '_'
            }
        })
        .collect()
}

/// Escapes a value for embedding inside a single-quoted JavaScript string literal.
pub fn escape_js_single_quoted(value: &str) -> String {
    let mut out = String::with_capacity(value.len() + 8);
    for c in value.chars() {
        match c {
            '\\' => out.push_str("\\\\"),
            '\'' => out.push_str("\\'"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            other => out.push(other),
        }
    }
    out
}

pub const CLI_TYPE_KIMI: &str = "kimi";
pub const KIMI_SUBTYPE_GLOBAL: &str = "kimi_api_global";
pub const KIMI_SUBTYPE_CHINA: &str = "kimi_api_china";
pub const KIMI_SUBTYPE_CODE: &str = "kimi_code";
pub const KIMI_SUBTYPE_LOCAL: &str = "kimi_local_cli";

/// Infers the Kimi provider subtype from its endpoint; other CLI types have no subtype.
pub fn infer_provider_subtype(cli_type: &str, endpoint: &str) -> &'static str {
    if cli_type != CLI_TYPE_KIMI {
        return "";
    }
    let lower = kt_trim(endpoint).to_lowercase();
    if lower.contains("api.kimi.com/coding") {
        KIMI_SUBTYPE_CODE
    } else if lower.contains("moonshot.cn") {
        KIMI_SUBTYPE_CHINA
    } else if starts_with_ignore_ascii_case(&lower, "http://127.0.0.1")
        || starts_with_ignore_ascii_case(&lower, "http://localhost")
        || starts_with_ignore_ascii_case(&lower, "http://10.0.2.2")
    {
        KIMI_SUBTYPE_LOCAL
    } else {
        KIMI_SUBTYPE_GLOBAL
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn strips_webview_ua_tokens() {
        let raw = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UP1A.231005.007; wv) \
                   AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.6778.104 Mobile Safari/537.36";
        assert_eq!(
            sanitize_user_agent(raw),
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UP1A.231005.007) \
             AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.104 Mobile Safari/537.36"
        );
    }

    #[test]
    fn ua_edge_cases_match_regex_semantics() {
        // `wv` not followed by `)` is kept; `XVersion/` has no word boundary; `Version/4` lacks a minor.
        assert_eq!(sanitize_user_agent("a; wv b"), "a; wv b");
        assert_eq!(sanitize_user_agent("XVersion/4.0 y"), "XVersion/4.0 y");
        assert_eq!(sanitize_user_agent("Version/4 y"), "Version/4 y");
        assert_eq!(sanitize_user_agent("(x;\t wv)"), "(x)");
        assert_eq!(sanitize_user_agent("  a   b \t c  "), "a b c");
        assert_eq!(sanitize_user_agent("Version/1.2.3 z"), ".3 z");
    }

    #[test]
    fn sanitizes_download_names() {
        assert_eq!(
            sanitize_download_file_name(" a/b:c?.pdf ", "", "x"),
            "a_b_c_.pdf"
        );
        assert_eq!(
            sanitize_download_file_name("downloadfile.bin", "image/PNG; q=1", "download_1"),
            "download_1.png"
        );
        assert_eq!(
            sanitize_download_file_name("", "", "download_2"),
            "download_2.bin"
        );
    }

    #[test]
    fn sanitizes_state_file_names_and_js() {
        assert_eq!(sanitize_state_file_name("tab_1/../é"), "tab_1_..__");
        assert_eq!(escape_js_single_quoted("a\\'b\n\r"), "a\\\\\\'b\\n\\r");
    }

    #[test]
    fn infers_kimi_subtypes() {
        assert_eq!(
            infer_provider_subtype("openai", "https://api.moonshot.cn"),
            ""
        );
        assert_eq!(
            infer_provider_subtype("kimi", "https://api.kimi.com/coding/v1"),
            KIMI_SUBTYPE_CODE
        );
        assert_eq!(
            infer_provider_subtype("kimi", "https://api.moonshot.cn/v1"),
            KIMI_SUBTYPE_CHINA
        );
        assert_eq!(
            infer_provider_subtype("kimi", " http://LOCALHOST:8080/v1"),
            KIMI_SUBTYPE_LOCAL
        );
        assert_eq!(
            infer_provider_subtype("kimi", "https://api.moonshot.ai/v1"),
            KIMI_SUBTYPE_GLOBAL
        );
    }
}
