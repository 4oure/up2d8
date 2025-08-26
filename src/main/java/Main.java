import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String BASE = "https://umd.instructure.com/api/v1";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5))
            .build();
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Pattern LINK_REL_RX = Pattern.compile("<([^>]+)>;\\s*rel=\"(\\w+)\"");

    private static String getKey() {
        String env = System.getenv("CANVAS_TOKEN");
        if (env != null && !env.isBlank()) return env.trim();
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("config.json")) {
            if (in == null) return null;
            String s = new String(in.readAllBytes());
            Map<String, String> m = new Gson().fromJson(s, new com.google.gson.reflect.TypeToken<Map<String, String>>() {
            }.getType());
            String k = m.get("canvas_api_key");
            return k == null ? null : k.trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean tokenWorks(String key) {
        okhttp3.OkHttpClient c = new okhttp3.OkHttpClient();
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url("https://umd.instructure.com/api/v1/users/self/profile")
                .header("Authorization", "Bearer " + key)
                .build();
        try (okhttp3.Response r = c.newCall(req).execute()) {
            return r.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        String key = getKey();
        System.out.println("Token prefix: " + (key == null ? "null" : key.substring(0, Math.min(10, key.length()))));
        if (!tokenWorks(key)) { System.err.println("Token not valid at runtime"); System.exit(1); }

        if (key == null || key.isEmpty()) {
            System.err.println("No API key found in resources/config.json under key canvas_api_key");
            System.exit(1);
        }
        File root = chooseDownloadDirectory();
        if (root == null) System.exit(1);
        List<JsonObject> courses = getPagedArray(BASE + "/courses?per_page=100&enrollment_state=active&include[]=term", key);
        if (courses == null) {
            System.err.println("No courses returned");
            System.exit(1);
        }
        for (JsonObject course : courses) {
            String courseId = getAsString(course, "id");
            String courseName = sanitize(getAsString(course, "name", "course-" + courseId));
            if (courseId == null) continue;
            Path courseDir = root.toPath().resolve(courseName);
            ensureDir(courseDir);
            Deque<JsonObject> stack = new ArrayDeque<>();
            List<JsonObject> top = getPagedArray(BASE + "/courses/" + courseId + "/folders?per_page=100", key);
            if (top == null) continue;
            for (JsonObject f : top) stack.push(f);
            while (!stack.isEmpty()) {
                JsonObject folder = stack.pop();
                String folderId = getAsString(folder, "id");
                String fullName = getAsString(folder, "full_name", getAsString(folder, "name", "folder-" + folderId));
                Path folderPath = courseDir.resolve(sanitizePath(fullName.replaceFirst("^/+", "")));
                ensureDir(folderPath);
                List<JsonObject> files = getPagedArray(BASE + "/folders/" + folderId + "/files?per_page=100", key);
                if (files != null) downloadFiles(files, folderPath, key);
                List<JsonObject> subs = getPagedArray(BASE + "/folders/" + folderId + "/folders?per_page=100", key);
                if (subs != null) for (JsonObject sf : subs) stack.push(sf);
            }
        }
        System.out.println("Done");
    }

    private static void downloadFiles(List<JsonObject> files, Path folderPath, String key) {
        for (JsonObject file : files) {
            String url = getAsString(file, "url");
            String displayName = getAsString(file, "display_name", getAsString(file, "filename", getAsString(file, "id")));
            if (url == null || displayName == null) continue;
            String safeName = sanitize(displayName);
            Path out = folderPath.resolve(safeName);
            if (Files.exists(out)) continue;
            Request req = new Request.Builder().url(url).header("Authorization", "Bearer " + key).build();
            try (Response resp = executeWith429Retry(req)) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    System.err.println("Skip " + safeName + " (" + resp.code() + ")");
                    continue;
                }
                try (InputStream in = resp.body().byteStream(); OutputStream os = Files.newOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
                }
                System.out.println("Downloaded " + out);
            } catch (IOException e) {
                System.err.println("Failed " + safeName + ": " + e.getMessage());
            }
        }
    }

    private static Response executeWith429Retry(Request request) throws IOException {
        int attempts = 0;
        while (true) {
            attempts++;
            Response r = client.newCall(request).execute();
            if (r.code() != 429) return r;
            String ra = r.header("Retry-After");
            r.close();
            long waitMs = 2000;
            if (ra != null) {
                try {
                    waitMs = Long.parseLong(ra) * 1000L;
                } catch (NumberFormatException ignored) {
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(Math.min(waitMs, 10000));
            } catch (InterruptedException ignored) {
            }
            if (attempts >= 5) return client.newCall(request).execute();
        }
    }

    private static List<JsonObject> getPagedArray(String url, String key) {
        List<JsonObject> out = new ArrayList<>();
        String next = url;
        while (next != null) {
            Request req = new Request.Builder().url(next).header("Authorization", "Bearer " + key).build();
            try (Response resp = client.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    System.err.println("HTTP " + resp.code() + " for " + next);
                    return out.isEmpty() ? null : out;
                }
                String body = resp.body().string();
                JsonElement el = JsonParser.parseString(body);
                if (!el.isJsonArray()) {
                    System.err.println("Non-array response at " + next);
                    return out.isEmpty() ? null : out;
                }
                List<JsonObject> page = gson.fromJson(el, new TypeToken<List<JsonObject>>() {
                }.getType());
                out.addAll(page);
                next = parseLinkNext(resp.header("Link"));
            } catch (IOException e) {
                System.err.println("Req error " + e.getMessage());
                return out.isEmpty() ? null : out;
            }
        }
        return out;
    }

    private static String parseLinkNext(String linkHeader) {
        if (linkHeader == null) return null;
        String[] parts = linkHeader.split(",\\s*");
        for (String p : parts) {
            Matcher m = LINK_REL_RX.matcher(p);
            if (m.find() && "next".equals(m.group(2))) return m.group(1);
        }
        return null;
    }



    private static File chooseDownloadDirectory() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Choose download directory");
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rv = ch.showSaveDialog(null);
        if (rv == JFileChooser.APPROVE_OPTION) return ch.getSelectedFile();
        return null;
    }

    private static void ensureDir(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String sanitize(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) t = "unnamed";
        return t;
    }

    private static String sanitizePath(String s) {
        if (s == null) return "folder";
        String[] parts = s.split("/+");
        List<String> clean = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            clean.add(sanitize(part));
        }
        if (clean.isEmpty()) return "folder";
        return String.join("/", clean);
    }

    private static String getAsString(JsonObject o, String key) {
        if (o == null || key == null) return null;
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return e.getAsString();
        return e.toString();
    }

    private static String getAsString(JsonObject o, String key, String def) {
        String v = getAsString(o, key);
        return v == null ? def : v;
    }
}
