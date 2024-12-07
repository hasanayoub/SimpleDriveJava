package tech.webclouds.simpledrivejava.helpers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class AwsV4Agent {

    private static final String ISO8601_DATE_TIME_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final String ISO8601_DATE_FORMAT = "yyyyMMdd";

    private final String service;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public AwsV4Agent(String service, String region, String accessKey, String secretKey) {
        this.service = service;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public HttpRequest prepareRequest(String url, String method, byte[] payload) throws Exception {
        payload = payload != null ? payload : new byte[0];
        method = method != null ? method : "GET";

        var requestBuilder = HttpRequest.newBuilder();
        URI uri = URI.create(url);
        requestBuilder.uri(uri);
        requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(payload));
//        requestBuilder.header("Host", uri.getHost());

        var utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        var amzLongDate = toIso8601(utcNow, ISO8601_DATE_TIME_FORMAT);
        var amzShortDate = toIso8601(utcNow, ISO8601_DATE_FORMAT);

        String payloadHash = payload.length > 0 ? hash(payload) : "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        requestBuilder.header("x-amz-date", amzLongDate);
        requestBuilder.header("x-amz-content-sha256", payloadHash);

        var signedHeaders = getSignedHeaders(requestBuilder);
        var canonicalRequest = buildCanonicalRequest(requestBuilder, uri, signedHeaders, payloadHash);
        var stringToSign = buildStringToSign(canonicalRequest, amzLongDate, amzShortDate);
        var signature = calculateSignature(amzShortDate, stringToSign);
        var authorizationValue = String.format("AWS4-HMAC-SHA256 Credential=%s/%s/%s/%s/aws4_request, SignedHeaders=%s, Signature=%s", accessKey, amzShortDate, region, service, signedHeaders, signature);
        requestBuilder.header("Authorization", authorizationValue);

        return requestBuilder.build();
    }

    private String buildCanonicalRequest(HttpRequest.Builder requestBuilder, URI uri, String signedHeaders, String payloadHash) {
        // Canonical URI: Encodes each part of the path
        String canonicalUri = Arrays.stream(uri.getPath().split("/")).map(this::encodeUri).collect(Collectors.joining("/"));

        // Canonical Query String: Sorts and encodes query parameters
        String canonicalQueryString = getCanonicalQueryParams(uri);

        // Canonical Headers: Collects and formats headers, sorts them by key
        HttpHeaders httpHeaders = requestBuilder.build().headers();

        // Ensure host header is included
        var hashMap = new HashMap<>(httpHeaders.map());
        hashMap.putIfAbsent("host", Collections.singletonList(uri.getHost()));

        String canonicalHeaders = hashMap.entrySet().stream().sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                .map(entry -> entry.getKey().toLowerCase() + ":" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("\n")) + "\n";

        // Debugging payload hash
        System.out.println("Payload Hash: " + payloadHash);

        // Combine all components into the canonical request
        return String.join("\n", requestBuilder.build().method(), canonicalUri, canonicalQueryString, canonicalHeaders, signedHeaders, payloadHash);
    }

    private String buildStringToSign(String canonicalRequest, String amzLongDate, String amzShortDate) {
        String credentialScope = String.join("/", amzShortDate, region, service, "aws4_request");
        return String.join("\n", "AWS4-HMAC-SHA256", amzLongDate, credentialScope, hash(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
    }

    private String calculateSignature(String amzShortDate, String stringToSign) throws Exception {
        byte[] dateKey = hmacSha256(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), amzShortDate);
        byte[] dateRegionKey = hmacSha256(dateKey, region);
        byte[] dateRegionServiceKey = hmacSha256(dateRegionKey, service);
        byte[] signingKey = hmacSha256(dateRegionServiceKey, "aws4_request");
        return toHexString(hmacSha256(signingKey, stringToSign));
    }

    private String getSignedHeaders(HttpRequest.Builder requestBuilder) {
        // Include the "host" header explicitly as it is required for signed headers
        return requestBuilder.build().headers().map().keySet().stream().map(String::toLowerCase).sorted()
                .collect(Collectors.joining(";", "host;", "")); // Prepend "host" to the sorted headers
    }

    private String getCanonicalQueryParams(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return "";
        }
        return Arrays.stream(query.split("&")).map(param -> param.split("=", 2)).sorted(Comparator.comparing(p -> URLEncoder.encode(p[0], StandardCharsets.UTF_8))).map(kv -> URLEncoder.encode(kv[0], StandardCharsets.UTF_8) + "=" + URLEncoder.encode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
    }

    private String encodeUri(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

    public static String hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to compute hash", e);
        }
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String toIso8601(Calendar calendar, String format) {
        var formatter = new java.text.SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(calendar.getTime());
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
