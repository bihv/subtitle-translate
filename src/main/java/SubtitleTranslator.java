import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SubtitleTranslator {
    private  static final Integer CHARACTER_PER_BATCH = 10000;
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=" + API_KEY;

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the input file path: ");
        String inputFilePath = scanner.nextLine();
        System.out.println("Enter the output file path: ");
        String outputFilePath = scanner.nextLine();

        // Read the entire SRT file content
        String srtContent = new String(Files.readAllBytes(Paths.get(inputFilePath)));

        // Split the SRT content into parts of about MAX_PART_SIZE items each
        List<String> parts = splitSRTContent(srtContent, CHARACTER_PER_BATCH);

        // Translate each part and save to file immediately
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {
            for (String part : parts) {
                String translatedPart = translateText(part);
                writer.write(translatedPart);
                writer.write("\n");
                writer.flush();
            }
        }
    }

    private static List<String> splitSRTContent(String srtContent, int charLimit) {
        String[] lines = srtContent.split("\n");
        List<String> parts = new ArrayList<>();
        StringBuilder partBuilder = new StringBuilder();
        int currentLength = 0;

        for (String line : lines) {
            if (currentLength + line.length() + 1 > charLimit && line.trim().isEmpty()) {
                parts.add(partBuilder.toString().trim());
                partBuilder.setLength(0);
                currentLength = 0;
            }
            partBuilder.append(line.replace("\"", "\\\"")).append("\n");
            currentLength += line.length() + 1;
        }

        if (partBuilder.length() > 0) {
            parts.add(partBuilder.toString().trim());
        }

        return parts;
    }

    private static String translateText(String text) throws IOException, ParseException, InterruptedException {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try (CloseableHttpClient client = org.apache.hc.client5.http.impl.classic.HttpClients.createDefault()) {
                HttpPost post = new HttpPost(GEMINI_API_URL);
                post.setHeader("Content-Type", "application/json");

                String prompt = "Translate the subtitles in this file into Vietnamese with the following requirements:\n" +
                        "\n" +
                        "Maintain the original format, including sequence numbers, timestamps, and the number of lines.\n" +
                        "The translations must match the context, culture, and situations occurring in the movie.\n" +
                        "Preserve the capitalization exactly as in the original text.\n" +
                        "Do not merge content from different timestamps into a single translation block.\n" +
                        "Return only the translated content in the specified format, without any additional explanations, introductions, or questions.\n" + text;

                String jsonRequest = String.format("""
                {
                   "contents": [
                     {
                       "role": "user",
                       "parts": [
                         {
                           "text": "%s"
                         }
                       ]
                     }
                   ],
                   "generationConfig": {
                     "temperature": 0.7,
                     "topK": 50,
                     "topP": 0.9,
                     "maxOutputTokens": 8192,
                     "responseMimeType": "text/plain"
                   }
                 }
                """, prompt);
                post.setEntity(new StringEntity(jsonRequest));

                String jsonResponse;
                JsonNode textNode = null;
                ObjectMapper mapper = new ObjectMapper();

                do {
                    try (CloseableHttpResponse response = client.execute(post)) {
                        jsonResponse = EntityUtils.toString(response.getEntity());
                        JsonNode rootNode = mapper.readTree(jsonResponse);
                        if (rootNode.path("candidates").isArray() && rootNode.path("candidates").size() > 0) {
                            JsonNode contentNode = rootNode.path("candidates").get(0).path("content");
                            if (contentNode.path("parts").isArray() && contentNode.path("parts").size() > 0) {
                                textNode = contentNode.path("parts").get(0).path("text");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                        Thread.sleep(1000); // Wait for 1 second before retrying
                    }
                } while (textNode == null || textNode.isNull());

                if (textNode != null && !textNode.isNull()) {
                    System.out.println("Translated text: " + textNode.asText());
                    return textNode.asText();
                }
            } catch (IOException e) {
                System.out.println("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
                Thread.sleep(1000); // Wait for 1 second before retrying
            }
            attempt++;
        }
        throw new IOException("Failed to translate text after " + maxRetries + " attempts");
    }
}