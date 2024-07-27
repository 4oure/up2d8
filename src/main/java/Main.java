import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.*;

import javax.swing.*;

public class Main {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String key = getKey();
        System.out.println("Beginning program with key found: " + key);

        File downloadDir = chooseDownloadDirectory();
        if (downloadDir != null) {
            processCourses(key, downloadDir);
        }
    }

    private static void processCourses(String key, File downloadDir) {
        List<JsonObject> courses = getCourses(key);
        if (courses != null) {
            for (JsonObject course : courses) {
                String courseID = course.get("id").getAsString();
                processFolders(courseID, key, downloadDir);
            }
        }
    }

    private static void processFolders(String courseID, String key, File downloadDir) {
        List<JsonObject> folders = getFolders(courseID, key);
        if(folders != null){
            for(JsonObject folder : folders){
                String folderID = folder.get("id").getAsString();
                processFiles(courseID, folderID, key, downloadDir);
            }
        }
    }

    private static void processFiles(String courseID, String folderID, String key, File downloadDir){
        List<JsonObject> files = getFiles(courseID, folderID, key);
        if(files != null){
            downloadFiles(files, downloadDir, key);
        }
    }

    private static List<JsonObject> getCourses(String apiKey) {
        return getPagedResponse("https://umd.instructure.com/api/v1/courses", apiKey);
    }

    private static List<JsonObject> getFolders(String courseId, String apiKey) {
        return getPagedResponse("https://umd.instructure.com/api/v1/courses/" + courseId + "/folders", apiKey);
    }

    private static List<JsonObject> getFiles(String courseId, String folderId, String apiKey) {
        return getPagedResponse("https://umd.instructure.com/api/v1/folders/" + folderId + "/files", apiKey);
    }

    private static List<JsonObject> getPagedResponse(String url, String apiKey) {
        List<JsonObject> results = new ArrayList<>();
        while (url != null) {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonElement e = JsonParser.parseString(responseBody);
                    if (e.isJsonArray()) {
                        List<JsonObject> pageResults = gson.fromJson(e, new TypeToken<List<JsonObject>>() {
                        }.getType());
                        results.addAll(pageResults);
                    } else {
                        System.out.println("Unexpected response format");
                        return null;
                    }

                    // pagination
                    url = getNextPageUrl(response);
                } else {
                    System.out.println("Failed to retrieve data, code: " + response.code());
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.print("Error finding  response");
                return null;
            }
        }
        return results;
    }

    private static String getNextPageUrl(Response response) {
        String linkHeader = response.header("Link");
        if (linkHeader != null) {
            String[] links = linkHeader.split(",");
            for (String link : links) {
                String[] parts = link.split(";");
                if (parts.length > 1 && parts[1].contains("rel=\"next\"")) {
                    return parts[0].replaceAll("<|>", "").trim();
                }
            }
        }
        return null;
    }

    private static void downloadFiles(List<JsonObject> files, File downloadDir, String apiKey) {
        for (JsonObject file : files) {
            if (file.has("url") && file.has("display_name")) {
                String url = file.get("url").getAsString();
                String filename = file.get("display_name").getAsString();
                if (url != null && !url.isEmpty()) {
                    Request request = new Request.Builder()
                            .url(url)
                            .header("Authorization", "Bearer " + apiKey)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            File fileToSave = new File(downloadDir, filename);
                            try (InputStream in = response.body().byteStream();
                                 FileOutputStream out = new FileOutputStream(fileToSave)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                                System.out.println("Downloaded file: " + fileToSave.getAbsolutePath());
                            }
                        } else {
                            System.out.println("Failed to download file: " + filename);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Invalid URL for file: " + filename);
                }
            } else {
                System.out.println("File object missing required fields");
            }
        }
    }

    private static String getKey() {
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("config.json");
        if (inputStream != null) {
            try {
                int data;
                StringBuilder sb = new StringBuilder();
                while ((data = inputStream.read()) != -1) {
                    sb.append((char) data);
                }
                String configData = sb.toString();
                Map<String, String> keyMap = gson.fromJson(configData, new TypeToken<Map<String, String>>() {
                }.getType());
                return keyMap.get("canvas_api_key");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.err.println("Resource not found: config.json");
        }
        return "";
    }

    private static File chooseDownloadDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            System.out.println("No directory selected.");
            System.exit(0);
            return null;
        }
    }
}
