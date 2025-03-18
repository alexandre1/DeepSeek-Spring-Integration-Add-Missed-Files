package com.deepseek_app.ai.deepseek_spring_integration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

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
                // Construct the request payload using ChatCompletionRequestContent

                analyzeImage();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        });
    }

    //TODDO Integrate DeepSeek Image when ready
    public MainView buildMainView(ChatClient.Builder builder) {
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
                // Construct the request payload using ChatCompletionRequestContent

                try {
                    var fileByte =  buffer.getInputStream().readAllBytes();
                    var receipt = client.prompt()
                            .user(userMessage -> userMessage
                                    .text("Please read the attached receipt and return the value in provided format")
                                    .media(
                                            MimeTypeUtils.parseMimeType(e.getMIMEType()),
                                            new InputStreamResource(buffer.getInputStream())
                                    )
                            )
                            .call()
                            .entity(Receipt.class);
                    showReceipt(receipt);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Notification.show("Error processing the file.");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                Notification.show("Error processing the file.");
            } finally {
                upload.clearFileList();
            }
        });

    }

    private void showReceipt(Receipt receipt) {
        var items = new Grid<>(LineItem.class);
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
    public void analyzeImage() throws JSONException {
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
                    .put("text", "What's in this image?"));
            content.add(mapper.createObjectNode()
                    .put("type", "image_url")
                    .set("image_url", mapper.createObjectNode()
                            .put("url", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg")));

            message.set("content", content);
            messages.add(message);

            // Add fields to the payload
            payload.put("model", "deepseek-chat");
            payload.set("messages", messages);
            payload.put("max_tokens", 300);

            // Print the payload
            System.out.println("Request Payload: " + payload.toPrettyString());

            // Create the HTTP request
            HttpPost request = new HttpPost("https://api.deepseek.com/v1/chat/completions");
            request.setEntity(new StringEntity(payload.toString()));
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", "Bearer " + apiKey); // Replace with your API key

            // Execute the request
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(request)) {
                    System.out.println("Status Code: " + response.getStatusLine().getStatusCode());
                    System.out.println("Response Body: " + EntityUtils.toString(response.getEntity()));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
