# DeepSeek-Spring-Integration 🌐🤖

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-brightgreen)](https://spring.io/)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

A seamless integration of DeepSeek's AI capabilities into Spring Boot applications. Add natural language processing, chatbots, and intelligent text generation to your Java projects with just a few lines of code.

**Demo Article**: [Medium Tutorial](https://medium.com/@saurav.kr.tech/integrating-deepseek-ai-with-spring-boot-a-step-by-step-guide-79e8a4972ebc)  
**API Documentation**: [DeepSeek API Docs](https://platform.deepseek.com/api-docs)

## Features ✨
- 🚀 Text generation via DeepSeek's language models
- 💬 Conversational AI chatbot implementation
- 
- ⚡ Spring Boot-native configuration
- 🔒 Secure API key management
- 📦 Ready-to-deploy REST API endpoints
## New features
- Implement non DeepSeek image text extraction
- Implement OCR to extraction of a reciept image
- Display the result in a MainView (Vaadin)
## Prerequisites 📋
- Java 17+
- Spring Boot 3.2+
- Maven/Gradle
- [DeepSeek API Key](https://platform.deepseek.com/api-keys)

## Quick Start 🚀

### 1. Clone the Repository
```bash
git clone https://github.com/sauravsku/DeepSeek-Spring-Integration.git
cd DeepSeek-Spring-Integration
```

### 2. Configure API Key
Create `src/main/resources/application.properties`:
```properties
deepseek.api.key=your_api_key_here
deepseek.api.url=https://api.deepseek.com/v1/chat/completions
```

### 3. Build & Run
```bash
mvn spring-boot:run
# or
./gradlew bootRun
```

## Usage Examples 💻

### Basic Text Generation
```bash
curl -X POST -H "Content-Type: application/json" \
-d '"Explain quantum computing in simple terms"' \
http://localhost:8080/api/ai/generate
```

### Sample Response
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "choices": [{
    "message": {
      "role": "assistant",
      "content": "Quantum computing uses quantum bits..."
    }
  }]
}
```

## Project Structure 📂
```
├── src
│   └── main
│       ├── java
│       │   └── com
│       │       └── deepseek
│       │           ├── config      # Configuration classes
│       │           ├── controller  # REST endpoints
│       │           └── service     # Business logic
│       └── resources
│           └── application.properties
├── LICENSE
└── README.md
```

## Advanced Configuration ⚙️
### Custom Model Parameters
```properties
# Optional parameters
deepseek.model=deepseek-chat
deepseek.temperature=0.7
deepseek.max_tokens=1000
```

### Enable Request Logging
```java
# Add to application.properties
logging.level.org.apache.http=DEBUG
```

## API Reference 📚
| Endpoint          | Method | Description                     |
|-------------------|--------|---------------------------------|
| `/api/ai/generate`| POST   | Process text generation requests|

**Request Body Format**:
```json
"Your prompt text here"
```

## Contributing 🤝
We welcome contributions! Please follow these steps:
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License 📄
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ by [Saurav Kumar](https://linkedin.com/in/your-profile)**  Changes by Alexandre Jaquet
[![GitHub Stars](https://img.shields.io/github/stars/sauravsku/DeepSeek-Spring-Integration?style=social)](https://github.com/sauravsku/DeepSeek-Spring-Integration/stargazers)

