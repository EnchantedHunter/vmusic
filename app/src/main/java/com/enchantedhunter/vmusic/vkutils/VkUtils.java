package com.enchantedhunter.vmusic.vkutils;

import android.content.Context;
import android.os.Environment;

import com.enchantedhunter.vmusic.data.Track;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.content.Context.MODE_PRIVATE;

public class VkUtils {

    private static final String API_BASE = "https://api.vk.com/method";

    private static final Gson gson = new Gson();
    private static final JsonParser parser = new JsonParser();

    private static HashMap<String, String> getHeader(){
        return new HashMap<String, String>(){{
            put("cache-control", "no-cache");
            put("x-get-processing-time", "1");
            put("x-vk-android-client", "new");
            put("user-agent", "VKAndroidApp/6.3-5277 (Android 6.0; SDK 23; armeabi-v7a; ZTE Blade X3; en; 1920x1080)");
        }};
    }
    private static byte[] getHeaderBytes(){
        return gson.toJson(getHeader()).getBytes();
    }

    public static String tryToLogin(String login, String pass) throws IOException {

        URL url = new URL(String.format("https://oauth.vk.com/token?" +
                "client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&libverify_support" +
                "=1&scope=all&v=5.123&lang=en&device_id=91090b1f4bd800af&grant_type=" +
                "password&username=%s&password=%s&2fa_supported=1", login, pass));
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        http.setFixedLengthStreamingMode(getHeaderBytes().length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        OutputStream os = http.getOutputStream();
        os.write(gson.toJson(getHeader()).getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;

        while((line = reader.readLine()) != null) {
            result.append(line);
        }

        return String.valueOf(gson.fromJson(result.toString(), Map.class).get("access_token"));
    }

    public static JsonElement request(String method, String token, Map parameters) throws Exception {

        if(token == null)
            throw new Exception("No auth!");

        if(parameters == null)
            parameters = new HashMap<String, String>();

        parameters.put("access_token", token);
        parameters.put("v", "5.123");
        parameters.put("device_id", "91090b1f4bd800af");
        parameters.put("lang", "en");

        StringBuilder postData = new StringBuilder();
        for (Object key : parameters.keySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode((String) key, "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(parameters.get(key)), "UTF-8"));
        }

        URL url = new URL(String.format("%s/%s", API_BASE, method));
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        http.setFixedLengthStreamingMode(postData.toString().getBytes().length);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("Content-Length", String.valueOf(postData.toString().getBytes().length));
        http.connect();

        OutputStream os = http.getOutputStream();
        os.write(postData.toString().getBytes());


        BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }

        return parser.parse(result.toString());
    }


    public static String requestAudio(String url) throws Exception {

        HttpURLConnection httpClient =
                (HttpURLConnection) new URL(url).openConnection();

        httpClient.setRequestMethod("GET");
        httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = httpClient.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        return response.toString();

    }

    public static byte[] requestChunk(String url) throws Exception {

        HttpURLConnection httpClient =
                (HttpURLConnection) new URL(url).openConnection();

        httpClient.setRequestMethod("GET");
        httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = httpClient.getResponseCode();

        byte[] b = new byte[2048];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataInputStream d = new DataInputStream((InputStream) httpClient.getContent());
        int c;
        while ((c = d.read(b, 0, b.length)) != -1) bos.write(b, 0, c);

        return bos.toByteArray();
    }

    private static Cipher getDecryptor(String key) throws Exception {

        byte[] iv = new byte[16];
        byte[] keyBytes = new byte[16];

        for(int i = 0 ; i < 16 ; i++) iv[i] = 0;

        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        System.arraycopy(key.getBytes("utf-8"), 0, keyBytes, 0, key.getBytes("utf-8").length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/NoPadding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        return cipherDecrypt;
    }

    public static boolean loadTrack(Track track, Context context) throws Exception {
        String url = track.getUrl();
        String playlist = requestAudio(url);

        Pattern pattern2 = Pattern.compile("/\\w*\\.m3u8");
        Matcher matcher2 = pattern2.matcher(url);
        matcher2.find();

        String baseUrl = url.substring(0, matcher2.end() - matcher2.group().length());

        List<String> playlistSplitedFilterd = new ArrayList<>();
        List<String> playlistSplited = Arrays.asList(playlist.split("#EXT-X-KEY"));

        for(int p = 0 ; p < playlistSplited.size() ; p ++) {
            if(playlistSplited.get(p).contains("#EXTINF")) {
                playlistSplitedFilterd.add(playlistSplited.get(p));
            }
        }

        Pattern pattern = Pattern.compile(":METHOD=AES-128,URI=\"(\\S*)\"");
        Matcher matcher = pattern.matcher(playlistSplitedFilterd.get(0));
        matcher.find();

        String key_url = matcher.group(1);
        String key = requestAudio(key_url);
        Cipher cipher = getDecryptor(key);

        for (String chunk : playlistSplitedFilterd) {
            List<byte[]> segments = new ArrayList<>();

            List<String> chunkSplt = Arrays.asList(chunk.split("#EXTINF"));

            for (String subChunk : chunkSplt) {

                Pattern pattern1 = Pattern.compile(":\\d+\\.\\d{3},(\\S*.ts\\S*)");
                Matcher matcher1 = pattern1.matcher(subChunk);

                while (matcher1.find()) {

                    byte[] audioSegment = requestChunk(baseUrl + "/" + matcher1.group(1));

                    segments.add(audioSegment);

                    if (chunk.contains("METHOD=AES-128")) {
                        byte[] data = cipher.update(segments.get(segments.size() - 1));
                        segments.set(segments.size() - 1, data);
                    }
                }

            }
            String fileName = String.format("%s-%s.ts", track.getTitle(), track.getArtist());


            File myFile = new File(Environment.getExternalStorageDirectory().toString() + "/" + fileName);

            if(!myFile.exists())
                myFile.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(myFile, true);
            for (int l = 0; l < segments.size(); l++)
                outputStream.write(segments.get(l));
            outputStream.close();

        }
        return true;
    }
}
