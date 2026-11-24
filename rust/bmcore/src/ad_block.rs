//! Host-based ad & tracker blocking, evaluated for every WebView sub-resource request.
//!
//! The host is extracted without allocating for ASCII URLs (the overwhelming majority) and then
//! matched, together with each parent domain, against a sorted table via binary search.

use crate::url::{before, kt_trim, starts_with_ignore_ascii_case};
use std::borrow::Cow;

/// Sorted (byte-wise) so lookups can use `binary_search`. A unit test enforces the ordering.
pub static AD_TRACKER_DOMAINS: &[&str] = &[
    "33across.com",
    "acecounter.com",
    "ad.daum.net",
    "ad.naver.com",
    "ad.planbplus.co.kr",
    "ad4989.co.kr",
    "adbay.co.kr",
    "adbrix.io",
    "adcr.naver.com",
    "adfit.kakao.com",
    "adform.net",
    "adition.com",
    "admaru.com",
    "admixer.co.kr",
    "adnmore.co.kr",
    "adnxs.com",
    "adop.cc",
    "ads-twitter.com",
    "ads.naver.com",
    "ads.tiktok.com",
    "adservice.google.com",
    "adsrvr.org",
    "advertising.com",
    "amazon-adsystem.com",
    "bidswitch.net",
    "casalemedia.com",
    "cauly.co.kr",
    "clickadu.com",
    "clickmon.co.kr",
    "criteo.com",
    "criteo.net",
    "dable.io",
    "doubleclick.net",
    "exoclick.com",
    "fsn.co.kr",
    "google-analytics.com",
    "googleadservices.com",
    "googlesyndication.com",
    "googletagmanager.com",
    "igaworks.com",
    "inmobi.com",
    "interworksmedia.co.kr",
    "media.net",
    "mediacategory.com",
    "mgid.com",
    "moatads.com",
    "mobon.net",
    "nasmedia.co.kr",
    "netinsight.co.kr",
    "openx.net",
    "outbrain.com",
    "pagead2.googlesyndication.com",
    "planbplus.co.kr",
    "popads.net",
    "propellerads.com",
    "pubmatic.com",
    "quantserve.com",
    "realclick.co.kr",
    "revcontent.com",
    "rubiconproject.com",
    "scorecardresearch.com",
    "sharethrough.com",
    "siape.veta.naver.com",
    "smartadserver.com",
    "taboola.com",
    "tappx.com",
    "targetpush.co.kr",
    "teads.tv",
    "triplelift.com",
    "widerplanet.com",
    "yieldmo.com",
];

#[inline]
fn is_listed(host: &str) -> bool {
    AD_TRACKER_DOMAINS.binary_search(&host).is_ok()
}

/// Extracts the lower-cased host of `url` (scheme, path, query, fragment and port removed).
pub fn extract_host(url: &str) -> Cow<'_, str> {
    let mut rest = kt_trim(url);
    // Sequential prefix removal, identical to the Kotlin `removePrefix` chain.
    if starts_with_ignore_ascii_case(rest, "https://") {
        rest = &rest[8..];
    }
    if starts_with_ignore_ascii_case(rest, "http://") {
        rest = &rest[7..];
    }
    let host = before(before(before(before(rest, '/'), '?'), '#'), ':');
    if host
        .bytes()
        .any(|b| b.is_ascii_uppercase() || !b.is_ascii())
    {
        Cow::Owned(host.to_lowercase())
    } else {
        Cow::Borrowed(host)
    }
}

/// True when the URL's host, or any of its parent domains, is a known ad/tracker domain.
pub fn is_ad_tracker_url(url: &str) -> bool {
    let host = extract_host(url);
    let host: &str = &host;
    if host.is_empty() {
        return false;
    }
    if is_listed(host) {
        return true;
    }
    let bytes = host.as_bytes();
    let last = bytes.len() - 1;
    for (i, &b) in bytes.iter().enumerate() {
        if b == b'.' && i < last && is_listed(&host[i + 1..]) {
            return true;
        }
    }
    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn table_is_sorted_and_unique() {
        assert!(AD_TRACKER_DOMAINS.windows(2).all(|w| w[0] < w[1]));
    }

    #[test]
    fn blocks_listed_hosts_and_subdomains() {
        assert!(is_ad_tracker_url(
            "https://pagead2.googlesyndication.com/pagead/js"
        ));
        assert!(is_ad_tracker_url(
            "https://sub.tracker.doubleclick.net:443/pixel?id=1"
        ));
        assert!(is_ad_tracker_url("https://www.googletagmanager.com/gtm.js"));
        assert!(is_ad_tracker_url("HTTPS://WWW.TABOOLA.COM/x"));
        assert!(is_ad_tracker_url("cdn.taboola.com#frag"));
        assert!(!is_ad_tracker_url("//cdn.taboola.com"));
        assert!(is_ad_tracker_url("ads.naver.com"));
    }

    #[test]
    fn allows_lookalikes_and_regular_sites() {
        assert!(!is_ad_tracker_url("https://notdoubleclick.net/article"));
        assert!(!is_ad_tracker_url("https://rust-lang.org/learn"));
        assert!(!is_ad_tracker_url("https://doubleclick.net.example.org/"));
        assert!(!is_ad_tracker_url(""));
        assert!(!is_ad_tracker_url("https:///path"));
        assert!(!is_ad_tracker_url("https://doubleclick.net."));
    }
}
