package tech.webclouds.simpledrivejava.helpers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.validator.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import tech.webclouds.simpledrivejava.errors.CustomProblemException;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    public static final String ONLY_ENGLISH_REGEX = "(^$|^[a-zA-Z0-9_-]+$)";
    public static final String EMAIL_REGEX = "(^$|^(.+)@(.+)$)";

    @SuppressWarnings("HardcodedFileSeparator")
//    public static final String URL_REGEX = "(^$|^(http:\\/\\/|https:\\/\\/)?(www.)?([a-zA-Z0-9]+).[a-zA-Z0-9]*.[a-z]{3}.?([a-z]+)?$)";

    public static String getClientIPAddressFromRequest(HttpServletRequest request) {
        try {
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            return ipAddress;
        } catch (Exception ex) {
            return "Unknown";
        }
    }


    public static ResponseEntity<String> readResponseFromHttp(HttpURLConnection httpURLConnection, int responseCode) throws IOException {

        @SuppressWarnings("HardcodedLineSeparator") final String SEPARATOR = "\r\n";

        StringBuilder responseBuilder = new StringBuilder();

        if (responseCode >= 200 && responseCode < 400) {
            try (Scanner scanner = new Scanner(httpURLConnection.getInputStream())) {
                while (scanner.hasNextLine()) responseBuilder.append(scanner.nextLine()).append(SEPARATOR);
            }
            return ResponseEntity.ok(responseBuilder.toString());
        } else {
            try (Scanner scanner = new Scanner(httpURLConnection.getErrorStream())) {
                while (scanner.hasNextLine()) responseBuilder.append(scanner.nextLine()).append(SEPARATOR);
            }
            return new ResponseEntity<>(responseBuilder.toString(), HttpStatus.valueOf(responseCode));
        }
    }

    private static final Policy POLICY;

    static {
        try (InputStream resourceAsStream = Utils.class.getResourceAsStream("/antisamy-slashdot.xml")) {
            if (resourceAsStream == null) {
                throw new IllegalStateException("Policy file not found");
            }
            POLICY = Policy.getInstance(resourceAsStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AntiSamy policy", e);
        }
    }

    public static String toSafeHtml(String html) throws ScanException, PolicyException {
        if (html == null) {
            return null;
        }

        AntiSamy antiSamy = new AntiSamy();
        CleanResults cleanResults = antiSamy.scan(html, POLICY);

        return cleanResults.getCleanHTML().trim();
    }

    public static String generateSmsVerificationCode() {
        return generateSmsVerificationCode(4);
    }

    public static String generateRandomCode(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public static String generateSmsVerificationCode(int length) {
        int random = 0;
        String randomString = "4583";
        int i = 0;
        while (random < 1000 && i < 100) {
            randomString = RandomStringUtils.randomNumeric(length);
            random = Integer.parseInt(randomString);
            i++;
        }
        return randomString;
    }

    public static String transferObjectToJsonString(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.writeValueAsString(object);
    }

    public static String tryTransferObjectToJsonString(Object object) {
        try {
            return transferObjectToJsonString(object);
        } catch (Exception ignored) {
            return object + " (could not be transferred json)";
        }
    }

    //      ObjectMapper mapper = new ObjectMapper();
    //        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    //        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    //        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    //        return mapper.writeValueAsString(object);
    //

    public static int convertFourDigitYearToTwoDigitYear(int year) throws ParseException {
        DateFormat safe = new SimpleDateFormat("yyyy");
        Date d = safe.parse(Integer.toString(year));
        DateFormat diff = new SimpleDateFormat("yy");
        return Integer.parseInt(diff.format(d));

    }

    public static String transferSerializableToJsonString(Serializable serializable) throws JsonProcessingException {
        return transferObjectToJsonString(serializable);
    }

    public static String getQuery(List<Map.Entry<String, String>> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(URLEncoder.encode(pair.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), StandardCharsets.UTF_8));
        }

        return result.toString();
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPost(String addressUrl, HashMap<String, String> headers, List<Map.Entry<String, String>> parameters, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        String query = getQuery(parameters);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Content-Length", Integer.toString(query.length()));
        return httpPost(addressUrl, headers, query, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPostWithoutHeader(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        return httpPost(addressUrl, headers, data, returnClass, errorClass, sslContext);
    }

    public static void ignoreSslVerification() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendOkPost(String addressUrl, HashMap<String, String> headers, Serializable payload, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) throws JsonProcessingException {
        String json = Utils.transferSerializableToJsonString(payload);
        return sendOkPost(addressUrl, headers, json, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendOkPost(String addressUrl, HashMap<String, String> headers, String jsonPayload, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {

        try {
            // Create HttpClient with optional SSLContext
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext);
            }

            try (HttpClient httpClient = clientBuilder.build()) {
                // Build the request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(addressUrl)).POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8)).header("Content-Type", "application/json");

                // Add headers
                headers.forEach(requestBuilder::header);

                HttpRequest httpRequest = requestBuilder.build();

                // Send the request
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                // Handle the response
                if (httpResponse.statusCode() == 200) {
                    String responseBody = httpResponse.body();
                    return ResponseEntity.ok(transferJsonStringToSerializable(responseBody, returnClass));
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return ResponseEntity.badRequest().build();
    }

    public static void trustAllCertificates() {
        // Set the truststore system property to an empty file
        System.setProperty("javax.net.ssl.trustStore", "empty_truststore.jks");
        // Disable hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext, boolean ignoreSsl) throws NoSuchAlgorithmException, KeyManagementException {
        headers.put("Content-Type", "application/json");
        if (ignoreSsl) {
            ignoreSslVerification();
        }
        return httpPost(addressUrl, headers, data, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<T> sendPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass) {
        ResponseEntity<? extends Serializable> responseEntity = httpPost(addressUrl, headers, data, returnClass, errorClass, null);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            Serializable body = responseEntity.getBody();
            // if body is instance of T then return it.
            if (returnClass.isInstance(body)) {
                T t = returnClass.cast(body);
                return ResponseEntity.ok(t);
            } else {
                return ResponseEntity.status(responseEntity.getStatusCode()).build();
            }
        } else {
            return ResponseEntity.status(responseEntity.getStatusCode()).build();
        }
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        headers.put("Content-Type", "application/json");
        return httpPost(addressUrl, headers, data, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass, boolean utf8, SSLContext sslContext) {
        if (utf8) {
            headers.put("Content-Type", "application/json;charset=utf-8");
        } else {
            headers.put("Content-Type", "application/json;");
        }
        return httpPost(addressUrl, headers, data, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<T> sendGet(String addressUrl, HashMap<String, String> headers, Class<T> returnClass, Class<E> errorClass) {
        ResponseEntity<? extends Serializable> responseEntity = sendGet(addressUrl, headers, returnClass, errorClass, null);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            Serializable body = responseEntity.getBody();
            // if body is instance of T then return it.
            if (returnClass.isInstance(body)) {
                T t = returnClass.cast(body);
                return ResponseEntity.ok(t);
            } else {
                return ResponseEntity.status(responseEntity.getStatusCode()).build();
            }
        } else {
            return ResponseEntity.status(responseEntity.getStatusCode()).build();
        }
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendGet(String addressUrl, HashMap<String, String> headers, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        return httpGet(addressUrl, headers, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPut(String addressUrl, HashMap<String, String> headers, List<Map.Entry<String, String>> parameters, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        String query = getQuery(parameters);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Content-Length", Integer.toString(query.length()));
        return httpPut(addressUrl, headers, query, returnClass, errorClass, sslContext);
    }

    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendPut(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        headers.put("Content-Type", "application/json");
        return httpPut(addressUrl, headers, data, returnClass, errorClass, sslContext);
    }

    public static <RETURN_TYPE extends Serializable, ERROR_TYPE extends Serializable> ResponseEntity<? extends Serializable> httpPut(String addressUrl, HashMap<String, String> headers, Serializable data, Class<RETURN_TYPE> returnClass, Class<ERROR_TYPE> errorClass, SSLContext sslContext) {
        try {
            // Create HttpClient with optional SSL context
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext);
            }
            try (HttpClient client = clientBuilder.build()) {
                // Convert data to JSON string
                String dataString;
                if (data instanceof String) {
                    dataString = (String) data;
                } else {
                    dataString = Utils.transferObjectToJsonString(data); // Assumes Utils handles serialization
                }

                // Create the HTTP request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(addressUrl)).PUT(HttpRequest.BodyPublishers.ofString(dataString, StandardCharsets.UTF_8));

                // Add headers
                headers.forEach(requestBuilder::header);

                HttpRequest request = requestBuilder.build();

                // Send the request
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Handle the response
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) { // Successful response
                    if (returnClass.equals(String.class)) {
                        return ResponseEntity.ok(response.body());
                    } else {
                        return ResponseEntity.ok(transferJsonStringToSerializable(response.body(), returnClass));
                    }
                } else { // Error response
                    return ResponseEntity.status(HttpStatus.valueOf(statusCode)).build();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.out);
        }
        return ResponseEntity.badRequest().build();
    }

    private static <RETURN_TYPE extends Serializable, ERROR_TYPE extends Serializable> ResponseEntity<? extends Serializable> httpPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<RETURN_TYPE> returnClass, Class<ERROR_TYPE> errorClass) {
        try {
            return httpPost(addressUrl, headers, data, returnClass, errorClass, SSLContext.getDefault());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }


    public static <T extends Serializable, E extends Serializable> ResponseEntity<? extends Serializable> sendHttpPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<T> returnClass, Class<E> errorClass, SSLContext sslContext) {
        try {
            // Create HttpClient with optional SSLContext
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext);
            }
            try (HttpClient client = clientBuilder.build()) {
                // Convert data to JSON string
                String dataString;
                if (data instanceof String) {
                    dataString = (String) data;
                } else {
                    dataString = Utils.transferObjectToJsonString(data); // Assumes Utils handles serialization
                }

                // Build the HTTP request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(addressUrl)).POST(HttpRequest.BodyPublishers.ofString(dataString, StandardCharsets.UTF_8));

                // Add headers
                headers.forEach(requestBuilder::header);

                HttpRequest request = requestBuilder.build();

                // Send the HTTP request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Process the response
                int responseCode = response.statusCode();
                if (responseCode >= 200 && responseCode < 300) { // Success
                    if (!returnClass.equals(String.class)) {
                        return ResponseEntity.ok(transferJsonStringToSerializable(response.body(), returnClass));
                    } else {
                        return ResponseEntity.ok(response.body());
                    }
                } else { // Error
                    if (!errorClass.equals(String.class)) {
                        return new ResponseEntity<>(Utils.transferJsonStringToSerializable(response.body(), errorClass), HttpStatus.valueOf(responseCode));
                    } else {
                        return new ResponseEntity<>(response.body(), HttpStatus.valueOf(responseCode));
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.out);
        }
        return ResponseEntity.badRequest().build();
    }


    public static <RETURN_TYPE extends Serializable, ERROR_TYPE extends Serializable> ResponseEntity<? extends Serializable> httpPost(String addressUrl, HashMap<String, String> headers, Serializable data, Class<RETURN_TYPE> returnClass, Class<ERROR_TYPE> errorClass, SSLContext sslContext) {
        try {
            // Create the HttpClient with optional SSLContext
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext);
            }
            try (HttpClient client = clientBuilder.build()) {
                // Convert the data to a JSON string
                String dataString;
                if (data instanceof String) {
                    dataString = (String) data;
                } else {
                    dataString = Utils.transferObjectToJsonString(data); // Assumes Utils handles serialization
                }

                // Build the HTTP request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(addressUrl)).POST(HttpRequest.BodyPublishers.ofString(dataString, StandardCharsets.UTF_8));

                // Add headers
                headers.forEach(requestBuilder::header);

                HttpRequest request = requestBuilder.build();

                // Send the HTTP request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Handle the response
                int responseCode = response.statusCode();
                if (responseCode >= 200 && responseCode < 300) { // Successful response
                    if (!returnClass.equals(String.class)) {
                        return ResponseEntity.ok(transferJsonStringToSerializable(response.body(), returnClass));
                    } else {
                        return ResponseEntity.ok(response.body());
                    }
                } else { // Error response
                    if (!errorClass.equals(String.class)) {
                        return new ResponseEntity<>(Utils.transferJsonStringToSerializable(response.body(), errorClass), HttpStatus.valueOf(responseCode));
                    } else {
                        return new ResponseEntity<>(response.body(), HttpStatus.valueOf(responseCode));
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.out); // Use proper logging in production
        }
        return ResponseEntity.badRequest().build();
    }

    public static <RETURN_TYPE extends Serializable, ERROR_TYPE extends Serializable> ResponseEntity<? extends Serializable> httpGet(String addressUrl, HashMap<String, String> headers, Class<RETURN_TYPE> returnClass, Class<ERROR_TYPE> errorClass, SSLContext sslContext) {
        try {
            // Create HttpClient with optional SSLContext
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext);
            }
            try (HttpClient client = clientBuilder.build()) {
                // Build the HTTP GET request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(addressUrl)).GET().header("accept", "application/json"); // Default header

                // Add custom headers
                headers.forEach(requestBuilder::header);

                HttpRequest request = requestBuilder.build();

                // Send the HTTP request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Handle the response
                int responseCode = response.statusCode();
                if (responseCode >= 200 && responseCode < 300) { // Successful response
                    return ResponseEntity.ok(transferJsonStringToSerializable(response.body(), returnClass));
                } else { // Error response
                    return ResponseEntity.status(HttpStatus.valueOf(responseCode)).build();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.out); // Use proper logging in production
        }
        return ResponseEntity.badRequest().build();
    }

    public static <T extends Serializable> T transferJsonBomStringToSerializable(String string, Class<T> serializableClass) throws IOException {
        // Create a JsonFactory
        JsonFactory factory = new JsonFactory();
        // Create a JsonParser for the JSON string, ensuring UTF-8 encoding
        JsonParser parser = factory.createParser(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.readValue(parser, serializableClass);
    }

    public static <T extends Serializable> T transferJsonStringToSerializable(String string, Class<T> serializableClass) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.readValue(string, serializableClass);
    }

    // transfer json string to List of Object <T>
    public static <T extends Serializable> List<T> transferJsonStringToList(String string, Class<T> serializableClass) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.readValue(string, mapper.getTypeFactory().constructCollectionType(List.class, serializableClass));
    }

    public static <T extends Serializable> T transferXmlStringToSerializable(String string, Class<T> serializableClass) throws NotActiveException {
        throw new NotActiveException();
    }

    @SuppressWarnings("unused")
    public static <T> T transferJsonStringToObject(String string, Class<T> objectClass) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.readValue(string, objectClass);
    }

    public static <T> T transferJsonStringToObject(String string, TypeReference<T> typeReference) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.readValue(string, typeReference);
    }

    public static boolean isEmptyOrNull(Object object) {
        return object == null || object.toString().trim().isEmpty();
    }

    public static double padDouble(double num, int pad) {
        try {
            num = num * Math.pow(10, pad);
            num = Math.round(num);
            return num / Math.pow(10, pad);
        } catch (RuntimeException ex) {
            return num;
        }
    }

    public static String padDoubleToString(double num, int pad) {
        try {
            num = num * Math.pow(10, pad);
            num = Math.round(num);
            String value = String.valueOf(num / Math.pow(10, pad));
            return value.endsWith(".0") ? value + "0" : value;
        } catch (RuntimeException ex) {
            return String.valueOf(num);
        }
    }

    public static Integer tryGetInteger(String integer) {
        try {
            return Integer.parseInt(integer);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Boolean tryGetBoolean(String bool) {
        try {
            return Boolean.parseBoolean(bool);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Double tryGetDouble(String number) {
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isOnVersion(HttpServletRequest request, int version) {
        try {
            Integer apiVersion = tryGetInteger(request.getHeader("X-API-VERSION"));
            if (apiVersion != null) {
                return apiVersion >= version;
            }
        } catch (RuntimeException ex) {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    public static Serializable transferBytesToSerializable(byte[] bytes) {
        Serializable stu = null;

        try (ByteArrayInputStream basis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(basis)) {
            stu = (Serializable) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace(System.out);
        }

        return stu;
    }

    public static String formatDecimal(Double number) {
        NumberFormat formatter = NumberFormat.getNumberInstance();
        return formatter.format(number);
    }

    @SuppressWarnings("unused")
    public static String reformatPhoneNumber(String phone) {
        return reformatPhoneNumber(phone, "%2B");
    }

    public static String reformatPhoneNumber(String phone, String prefix) {
        if (phone != null) {

            phone = phone.trim();

            if (phone.startsWith("00")) {
                phone = prefix + phone.substring(2);
            }

            //noinspection DuplicateCondition
            if (!phone.startsWith("+")) {
                phone = prefix + phone;
            } else //noinspection DuplicateCondition
                if (phone.startsWith("+")) {
                    phone = prefix + phone.substring(1);
                }
        }

        return phone;
    }

    public static String convertHindiNumbersToEnglish(String hindiNumber) {

        if (hindiNumber == null) {
            return null;
        }
        StringBuilder newNumber = new StringBuilder();
        for (int i = 0; i < hindiNumber.length(); i++) {
            int code = hindiNumber.charAt(i);
            String chars = switch (code) {
                case 1632 -> "0";
                case 1633 -> "1";
                case 1634 -> "2";
                case 1635 -> "3";
                case 1636 -> "4";
                case 1637 -> "5";
                case 1638 -> "6";
                case 1639 -> "7";
                case 1640 -> "8";
                case 1641 -> "9";
                default -> Character.toString((char) code);
            };
            newNumber.append(chars);
        }
        return newNumber.toString();
    }


    @SuppressWarnings("unused")
    public static byte[] transferSerializableToBytes(Serializable serializable) {
        byte[] stream = null;
        try (ByteArrayOutputStream bass = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bass)) {
            oos.writeObject(serializable);
            stream = bass.toByteArray();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return stream;
    }

    public static void logCurrentTime(Class<?> clazz, String method, int line) {
        System.out.println("Log Time " + System.currentTimeMillis() + ": " + clazz.getName() + "/" + method + "[" + line + "]");
    }

    public static String convertMarkdownToHTML(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
        return htmlRenderer.render(document);
    }

    public static boolean isEmpty(String businessCategory) {
        return businessCategory == null || businessCategory.trim().isEmpty();
    }

    public static String formatDate(Date now, String s) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(s);
        return simpleDateFormat.format(now);
    }

    public static Date subDays(Date now, int i) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, -i);
        return calendar.getTime();
    }

    public static void printWarning(Class<?> clazz, Exception ex) {
        try {
            System.out.println("WARNING (" + clazz.getName() + "): " + ex.getMessage());
        } catch (Exception ignored) {
        }
    }

    @SafeVarargs
    public static void assertAtLeastOne(Map.Entry<String, String>... fields) {
        StringBuilder stringBuilder = new StringBuilder();

        for (var field : fields) {
            stringBuilder.append(field.getKey()).append(", ");
            if (field.getValue() != null && !field.getValue().isEmpty()) {
                return;
            }
        }
        throw new CustomProblemException(400, "At least one of the following fields is required: " + stringBuilder);
    }

    public static boolean isBase64(String value) {
        if (value == null || value.length() % 4 != 0) {
            return false;
        }
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Date convertToDateViaInstant(LocalDateTime dueDate) {
        return Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static boolean isValidURL(String urlStr) {
        try {
            // Trim the URL to remove leading/trailing spaces
            if (urlStr == null || urlStr.isBlank()) {
                return false;
            }

            urlStr = urlStr.trim();

            // If the URL does not start with "http://" or "https://", prepend "https://"
            //noinspection HttpUrlsUsage
            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                urlStr = "https://" + urlStr;
            }

            // Build the URI
            URI uri = URI.create(urlStr);

            // Create HttpClient
            try (HttpClient client = HttpClient.newHttpClient()) {
                // Build a HEAD request to verify the URL
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();

                // Send the request
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

                // Return true for HTTP status codes indicating success
                return response.statusCode() >= 200 && response.statusCode() < 400;
            }
        } catch (Exception e) {
            // Return false for any exceptions (e.g., invalid URI or unreachable URL)
            return false;
        }
    }

    public static void assertUrlOrEmpty(String... urls) {
        for (String url : urls) {
            if (url != null && !url.isEmpty()) {
                if (!isValidURL(url)) {
                    throw new CustomProblemException(400, "One of the URL is invalid");
                }
            }
        }
    }

    public static String removeInvalidJsonCharacters(String json) {
        return json.replaceAll("[^\\x00-\\x7F]", "").replace(" ", "");
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <X> ResponseEntity<X> wrapOrNotFound(Optional<X> maybeResponse, HttpHeaders header) {
        return maybeResponse.map(response -> ResponseEntity.ok().headers(header).body(response)).orElse(new ResponseEntity<>(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value())));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <X> ResponseEntity<X> wrapOrNotFound(Optional<X> maybeResponse) {
        return wrapOrNotFound(maybeResponse, null);
    }

}
