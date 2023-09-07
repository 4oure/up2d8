import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		String key = getKey();
		System.out.println("Beginning program with key found: " + key);
		sendAPIReq();

	}

	private static void sendAPIReq() {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
			.url("https://umd.instructure.com/files")
			.header("Authorization:", "Bearer " + getKey())
			.build();

		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				String responseBody = response.body().string();
				System.out.println("API Response: " + responseBody);
			} else {
				System.out.println("Failed, code: " + response.code());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getKey() {
		StringBuilder sb = new StringBuilder();
		try {
			File myFile = new File("/Users/gavin/eclipse-workspace/up2d8/key.txt");
			Scanner scanner = new Scanner(myFile);
			while (scanner.hasNext()) {
				sb.append(scanner.next());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}
}

