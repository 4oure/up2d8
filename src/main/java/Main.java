import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.*;

public class Main {

	public static void main(String[] args) {

		String key = getKey();
		System.out.println("Beginning program with key found: " + key);
		sendAPIReq();

	}


	private static void sendAPIReq() {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
			.method("GET", null)
			.url("https://umd.instructure.com/api/v1/courses")
			//.url("https://umd.instructure.com/api/v1/courses/1354011/folders")
			.header("Authorization", "Bearer " + getKey())
			.build();

		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				String responseBody = response.body().string();
				//	System.out.println("API Response: " + responseBody);
				Gson gson = new Gson();
				List<JsonObject> myObjects = new ArrayList<>();
				JsonElement e = JsonParser.parseString(responseBody);
				if (e.isJsonArray()) {
					JsonArray array = e.getAsJsonArray();

					for (JsonElement element : array) {
						JsonObject myObj = gson.fromJson(element, JsonObject.class);
						myObjects.add(myObj);

					}
				} else if (e.isJsonObject()) {
					JsonObject myObj = gson.fromJson(e, JsonObject.class);
					myObjects.add(myObj);
				}
				for (JsonObject obj : myObjects) {
					String prettyJson = gson.toJson(obj);
					System.out.println(prettyJson);
					Course course = gson.fromJson(obj, Course.class);
					System.out.println("Course name: " + course.getName());
					System.out.println(course.getAccount_id());
				}
			} else {
				System.out.println("Failed, code: " + response.code());
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getKey() {
//		StringBuilder sb = new StringBuilder();
//		try {
//			File myFile = new File("/Users/gavin/eclipse-workspace/up2d8/key.txt");
//			Scanner scanner = new Scanner(myFile);
//			while (scanner.hasNext()) {
//				sb.append(scanner.next());
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//
//		return sb.toString();

		ClassLoader cl = Main.class.getClassLoader();
		InputStream is = cl.getResourceAsStream("config.json");
		if (is != null) {
			try {
				int data;
				StringBuilder sb = new StringBuilder();
				while ((data = is.read()) != -1) {
					sb.append((char) data);
				}
				String configData = sb.toString();
				Gson g = new Gson();
				Map<String, String> keyMap = g.fromJson(configData, new TypeToken<Map<String, String>>() {
				}.getType());
				return keyMap.get("canvas_api_key");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
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
}

