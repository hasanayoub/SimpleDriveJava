# **Installation and Configuration Steps**

---

## **1. Create Project Directory**
- Create a directory for your project and clone the repository:
  ```
  mkdir simple-drive-java
  cd simple-drive-java
  git clone https://github.com/hasanayoub/SimpleDriveJava.git
  cd SimpleDriveJava
  ```
---

## **2. Install Java and Maven**
- Ensure you have Java 17 or higher installed:
  ```
  java -version
  ```
- Install Maven:
  ```
  sudo apt-get install maven
  mvn -version
  ```
---

## **3. Configure `application-localhost.yaml`**
- Copy the provided `application-localhost.yaml` to your desired environment configuration file:
    ```
    cp src/main/resources/application-localhost.yaml src/main/resources/application-<ENV>.yaml
    ```
    - Replace `<ENV>` with the environment name (e.g., `production`, `testing`).

- **Encryption for Sensitive Data**:
    - Use the `appSecret` provided in the configuration:
      ```
      app:
        appSecret: YjJMB9y4shdriveS4ME+HN==
      ```
    - Use [Jasypt Online Encryption Tool](https://www.javainuse.com/jasypt) for encrypting sensitive data like:
        - Database `username`
        - Database `password`

    - Replace the values in the YAML with `ENC(encrypted-value)`:
      ```
      username: ENC(encrypted-username)
      password: ENC(encrypted-password)
      ```
---

## **4. Configure Database**
- Update the `spring.datasource.url`:
  ```
  url: jdbc:mysql://<DATABASE_SERVER>/<DATABASE_NAME>?useUnicode=true...
  ```
    - Replace `<DATABASE_SERVER>` with the server hostname (e.g., `localhost`).
    - Replace `<DATABASE_NAME>` with your database name (e.g., `simple_drive`).

- Create the database manually:
  ```
  CREATE DATABASE simple_drive;
	```
---

## **5. Modify Application Configuration**
- **StorageType**:
    - Choose a value for `StorageType` based on your setup:
      ```
      StorageType: S3 # S3, Local, Database, Ftp
      ```
- **JWT Token**:
    - Update `JwtSecretKey` with a strong key:
      ```
      JwtSecretKey: S9@q(cJE2AgfM%PwI(pZSXjNBwH#W&mp
      ```	
- **S3 Configuration**:
    - Set values for:
      ```
      BucketUrl: <S3_BUCKET_URL>
      AccessKey: <S3_ACCESS_KEY>
      SecretKey: <S3_SECRET_KEY>
      Region: <S3_REGION>
      ```
---

## **6. Build the Application**
- Compile the application using Maven:
  ```
  mvn clean install
  ```
---

## **7. Run the Application**
- Use the desired profile to start the application:
  ```
  mvn spring-boot:run -Dspring-boot.run.profiles=localhost
  ```
---

## **8. Testing the Application**
- Update the `Testing` section in `application-localhost.yaml`:
    ```
    Testing:
      FilePath: <PATH_TO_TEST_FILE>
      ServerUrl: <SERVER_URL>
      FileHashValue: <EXPECTED_FILE_HASH>
      Username: <USERNAME>
      Password: <PASSWORD>
    ```
    - Replace placeholders with your values.

---

# **Explanation of Configuration Fields**

## **Database Section**
- **`url`**: JDBC URL to connect to the database.
    - Example:
      ```
      jdbc:mysql://<DATABASE_SERVER>/<DATABASE_NAME>
      ```
- **`username` and `password`**: Encrypted credentials.

---

## **appSecret**
- Used as the encryption key for sensitive data.

---

## **StorageType**
- Specifies where files are stored: `S3`, `Local`, `Database`, or `Ftp`.

---

## **JWT Token**
- **`TokenIssuer`**: Issuer of the JWT.
- **`JwtSecretKey`**: Secret key for signing tokens.
- **`TokenAudience`**: Expected audience for the token.

---

## **S3 Configuration**
- **`BucketUrl`**: URL of the S3 bucket.
- **`AccessKey` and `SecretKey`**: S3 credentials.
- **`Region`**: Region of the S3 bucket.

---

## **Testing Section**
- **`FilePath`**: Path to a file for testing uploads.
- **`ServerUrl`**: URL for backend test API.
- **`FileHashValue`**: Expected hash value of the file.

---

# **Run Command**
- Use the following command to start the application with a profile:
  ```
  mvn spring-boot:run -Dspring-boot.run.profiles=localhost
  ```
