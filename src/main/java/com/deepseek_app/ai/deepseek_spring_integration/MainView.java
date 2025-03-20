package com.deepseek_app.ai.deepseek_spring_integration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Image;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import net.sourceforge.tess4j.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.parameters.P;
import org.springframework.web.multipart.MultipartFile;

@Route("")
@PageTitle("Receipt")
@Menu(title = "Receipt", order = 1)
public class MainView extends VerticalLayout {

    public record LineItem(String name, int quantity, BigDecimal price) {}

    public record Receipt(String merchant, BigDecimal total, List<LineItem> lineItems) {}
    @Autowired
    private Environment env;

    private Component previousPhoto;
    private Paragraph photoName;

    @Value("${springai}")
    private String apiKey;

    public String imageUrl;
    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }


    public MainView(ChatClient.Builder builder) {
        var client = builder.build();
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        var output = new Div();

        Text instructions = new Text("Upload an image of a receipt. The AI will extract the details and show them below.");
        add(instructions, upload, output);

        upload.setAcceptedFileTypes("image/*");
        upload.addSucceededListener(e -> {
            try {
                InputStream inputStream = buffer.getInputStream();
                byte[] bytes = IOUtils.toByteArray(inputStream);

                // Save the file to the server
                File targetFile = new File(getAppPath() + e.getFileName());
                FileUtils.copyInputStreamToFile(new ByteArrayInputStream(bytes), targetFile);

                // Display the uploaded image
                Component component = createComponent(e.getMIMEType(), e.getFileName(), bytes);
                showOutput(e.getFileName(), component, output);

                // Call the modified singleFileUpload method
                String ocrText = singleFileUpload(buffer, e.getFileName());
                System.out.println("OCR Text: " + ocrText);
                Receipt receipt = parseReceiptFromOCR(ocrText);

                showReceipt(receipt);
                Paragraph out = new Paragraph();
                out.setText(ocrText);
                // add(out);
                //analyzeImage(targetFile, buffer);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        });
    }



    private void showReceipt(Receipt receipt) {
        var items = new Grid<>(LineItem.class);
        List<LineItem> list =  receipt.lineItems;
        items.setItems(receipt.lineItems());

        add(
                new H3("Receipt details"),
                new Paragraph("Merchant: " + receipt.merchant()),
                new Paragraph("Total: " + receipt.total()),
                items
        );
    }

    private Component createComponent(String mimeType, String fileName, byte[] bytes) {
        if (mimeType.startsWith("image")) {
            Image image = new Image();
            image.setMaxWidth("100%");
            image.setSrc(new StreamResource(fileName, () -> new ByteArrayInputStream(bytes)));
            return image;
        } else {
            Div content = new Div();
            String text = String.format(
                    "Mime type: '%s'\nSHA-256 hash: '%s'",
                    mimeType,
                    generateSHA256Hash(bytes)
            );
            content.setText(text);
            return content;
        }
    }

    private String generateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private void showOutput(String text, Component content, HasComponents outputContainer) {
        if (photoName != null) {
            outputContainer.remove(photoName);
        }
        if (previousPhoto != null) {
            outputContainer.remove(previousPhoto);
        }
        photoName = new Paragraph(text);
        outputContainer.add(photoName);
        previousPhoto = content;
        outputContainer.add(previousPhoto);
    }

    private String getAppPath() {
        String path = env.getProperty("spring.servlet.multipart.location");
        if (path == null) {
            throw new IllegalStateException("spring.servlet.multipart.location property is not set.");
        }
        return path;
    }

    public static String getFlowerImage() throws Exception {
        // Assuming the image is in the same directory as the Java file
        Path imagePath = Paths.get("src/receipt.jpg");
        byte[] imageBuffer = Files.readAllBytes(imagePath);
        String base64Image = Base64.getEncoder().encodeToString(imageBuffer);
        return "data:image/jpeg;base64," + base64Image;
    }

    public void analyzeImage(File targetFile,MemoryBuffer buffer) throws JSONException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Create the messages array
            ArrayNode messages = mapper.createArrayNode();
            ObjectNode message = mapper.createObjectNode();
            message.put("role", "user");

            // Create the content array
            ArrayNode content = mapper.createArrayNode();
            content.add(mapper.createObjectNode()
                    .put("type", "text")
                    .put("text", "Please read the attached receipt and return the value in provided receipt record java to json format"));
            content.add(mapper.createObjectNode()
                    .put("type", "image_url")
                    .set("image_url", mapper.createObjectNode()
                            .put("url", "data:image/jpeg;base64," + getFlowerImage())));
            message.set("content", content);
            messages.add(message);

            // Add fields to the payload
            payload.put("model", "deepseek/deepseek-r1:free");
            payload.set("messages", messages);
            payload.put("max_tokens", 1000);

            // Print the payload
            System.out.println("Request Payload: " + payload.toPrettyString());

            // Create the HTTP request
            HttpPost request = new HttpPost("https://openrouter.ai/api/v1/chat/completions");
            request.setEntity(new StringEntity(payload.toString()));
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", "Bearer " + apiKey); // Replace with your API key

            // Execute the request
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(request)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    System.out.println("Response Body: " + responseBody);

                    // Parse the response
                    ObjectNode responseJson = (ObjectNode) mapper.readTree(responseBody);
                    String ocrText = responseJson.path("choices").get(0).path("message").path("content").asText();

                    // Map the OCR text to the Receipt object
                    Receipt receipt = parseReceiptFromOCR(ocrText);

                    showReceipt(receipt);
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Receipt parseReceiptFromOCR(String ocrText) {
        // Implement logic to parse the OCR text and extract merchant, total, and line items
        // Example:
        String merchant = extractMerchant(ocrText);
        BigDecimal total = extractTotal(ocrText);
        List<LineItem> lineItems = extractLineItems(ocrText);
        return new Receipt(merchant, total, lineItems);
    }

    private String extractMerchant(String ocrText) {
        String[] patterns = {"Merchant", "Store", "Vendor", "Market", "Receipt from","MARKET"};
        for (String pattern : patterns) {
            int index = ocrText.indexOf(pattern);
            if (index != -1) {
                String merchantLine = ocrText.substring(index + pattern.length()).trim();
                // Extract the entire line as the merchant name
                return merchantLine.split("\n")[0].trim();
            }
        }
        return "Unknown Merchant";
    }

    private BigDecimal extractTotal(String ocrText) {
        String[] patterns = {"Subtotal:", "Net Sales:", "Amount Due:", "Grand Total:", "Net Sales:", "Tax:", "Total :"};
        for (String pattern : patterns) {
            int index = ocrText.toLowerCase().indexOf(pattern.toLowerCase());
            if (index != -1) {
                String totalLine = ocrText.substring(index + pattern.length()).trim();
                // Use a regular expression to find the numeric value (including commas)
                java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("[\\d,]+(?:\\.\\d{2})?");
                java.util.regex.Matcher matcher = amountPattern.matcher(totalLine);
                if (matcher.find()) {
                    String amount = matcher.group().replace(",", ""); // Remove commas
                    return new BigDecimal(amount);
                }
            }
        }
        throw new IllegalArgumentException("Total amount not found in OCR text");
    }

    private List<LineItem> extractLineItems(String ocrText) {
        List<LineItem> lineItems = new ArrayList<>();
        String[] lines = ocrText.split("\n");

        for (String line : lines) {
            if (line.matches(".*\\$?\\d+(\\.\\d{2})?.*")) {
                // Extract the item name and price
                String[] parts = line.split("\\$");
                if (parts.length >= 2) {
                    String itemPart = parts[0].trim();
                    // Extract the first word with only uppercase letters
                    String firstAllUppercaseWord = extractFirstUppercaseWordsUntilSpace(itemPart);

                    if (firstAllUppercaseWord != null  && firstAllUppercaseWord.length() > 0) {
                        System.out.println("ITEM PART : " + firstAllUppercaseWord);
                    } else {
                        firstAllUppercaseWord = itemPart;
                        System.out.println("ITEM PART CREATE: " + itemPart);
                    }

                    String pricePart = "$" + parts[1].trim();
                    int quantity = 1; // Default quantity

                    if (firstAllUppercaseWord != null) {


                        // Check if the line starts with "Qty" (case-insensitive)
                        if (itemPart.toLowerCase().startsWith("qty")) {
                            // Extract the quantity
                            String[] qtyParts = itemPart.split("\\s+");
                            if (qtyParts.length >= 2) { // Ensure there is a quantity value after "Qty"
                                try {
                                    quantity = Integer.parseInt(qtyParts[1]); // Parse the quantity
                                    System.out.println("QUANTITY FOUND : " + quantity);
                                } catch (NumberFormatException e) {
                                    // If quantity parsing fails, default to 1
                                    System.out.println("QUANTITY PARSING FAILED, USING DEFAULT: 1");
                                }
                            }
                        }
                    }
                    if (quantity > 0) {
                        // Extract the price
                        java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("\\$?([\\d,]+(?:\\.\\d{2})?)");
                        java.util.regex.Matcher matcher = pricePattern.matcher(pricePart);
                        if (matcher.find()) {
                            String priceStr = matcher.group(1).replace(",", "");
                            BigDecimal price = new BigDecimal(priceStr);
                            System.out.println("PRICE FOUND : " + price);
                            lineItems.add(new LineItem(firstAllUppercaseWord, quantity, price));
                        } else {
                            System.out.println("NO PRICE FOUND");
                        }
                    }
                }
            }
        }
        return lineItems;
    }

    public static String extractFirstUppercaseWordsUntilSpace(String input) {
        // Split the input string into words based on whitespace
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();

        // Iterate through each word
        for (String word : words) {
            // Check if the word contains only uppercase letters
            if (isAllUppercase(word)) {
                // Append the word to the result with a space
                result.append(word).append(" ");
            } else {
                // Stop when a word does not contain only uppercase letters
                break;
            }
        }

        // Remove the trailing space (if any) and return the result
        return result.toString().trim();
    }

    public static boolean isAllUppercase(String word) {
        // Iterate through each character in the word
        for (char ch : word.toCharArray()) {
            // If any character is not an uppercase letter, return false
            if (!Character.isUpperCase(ch)) {
                return false;
            }
        }
        return true; // All characters are uppercase
    }
    public String singleFileUpload(MemoryBuffer buffer, String fileName) throws IOException, TesseractException {
        InputStream inputStream = buffer.getInputStream();
        byte[] bytes = IOUtils.toByteArray(inputStream);

        // Save the file to the server
        Path path = Paths.get("E://upload/" + fileName);
        Files.write(path, bytes);

        File convFile = new File(fileName);
        FileUtils.copyInputStreamToFile(new ByteArrayInputStream(bytes), convFile);

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("E:\\Program Files\\Tesseract-OCR\\tessdata");
        String text = tesseract.doOCR(convFile);
        tesseract.setLanguage("eng");

        System.out.println(text);
        System.out.println(convFile);

        return text;
    }
    public String result() {
        return "result";
    }
    private void run(MultipartFile file) {
        // Implement the logic you want to execute with the file
        System.out.println("Running additional operations on file: " + file.getOriginalFilename());
    }
    public static File convert(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());

        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());

        fos.close();
        return convFile;
    }

}