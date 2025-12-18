## 🚀 Getting Started

### 🛠️ Prerequisites
- **JDK 17**: Ensure you have Java Development Kit 17 installed.
- **Maven 3.9.x**: Install Apache Maven version 3.9.x. Verify with `mvn -v`.

---

### 📦 Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/rSparsh/Unified-Marketing-Gateway
   cd Unified-Marketing-Gateway
   ```

2. **Create Secrets File**  
   Follow the steps mentioned in the application-secrets.example.yaml file to create your own secrets file.
   Edit `application-secrets.yaml` to include your configuration (e.g., API keys, database credentials).


3. **Build the Project**  
   Run the Maven build to compile and package the application:
   ```bash
   mvn clean install
   ```

4. **Run the Application**  
   Execute the main class `UnifiedMarketingGatewayApplication` using your IDE or via the command line:
   ```bash
   mvn spring-boot:run
   ```
   *(If using an IDE, locate the `UnifiedMarketingGatewayApplication` class and run it as a Java application.)*

5. **Test with Postman**
    - Follow the steps mentioned in [Local_Testing Wiki](local_testing.txt) into Postman.
    - Replace placeholders in the imported requests with your real values.
    - Send the requests and validate the responses.

6. **View the DB in H2-Console**
    - Once your application is up & running, open this URL in your browser: `localhost:8080/h2-console`

    In the opened UI, enter these credentials to view the h2-console

    ```bash
         +-----------------------------------------+
         |    H2 Console Login                     |
         |-----------------------------------------|
         | Driver Class:    org.h2.Driver          |
         | JDBC URL:        jdbc:h2:mem:whatsappdb |
         | User Name:       sa                     |
         | Password:        (leave empty)          |
         | Connect Button                          |
         +-----------------------------------------+
   ```
---

### 📌 Notes
- Ensure `application-secrets.yaml` is placed in the correct directory (e.g., `src/main/resources` if using Spring Boot).
- If encountering build issues, verify your JDK and Maven versions match the requirements.
