# Cost Buddy

Cost Buddy is a manual cloud billing audit tool for finding unfamiliar, long-running pay-as-you-go cost items in Alibaba Cloud bills.

The project is initialized with the same stack as `motherdata`: Spring Boot, Gradle, MyBatis XML, Flyway, MySQL, and a Vite + React + TypeScript frontend.

## Local Development

Backend:

```bash
./gradlew bootRun
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Default metadata database:

```text
jdbc:mysql://127.0.0.1:3306/costbuddy
username: origin
password: 123258
```
