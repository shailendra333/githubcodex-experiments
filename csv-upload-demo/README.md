# CSV Customer Upload Demo — Spring Boot

A **Spring Boot 3** application that demonstrates production-grade techniques for
uploading and processing **large CSV files** (hundreds of MB) into a **SQLite** database.

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+

### Run
```bash
cd csv-upload-demo
mvn spring-boot:run
```
Open → **http://localhost:8080**

---

## 🗂 Project Structure

```
src/main/java/com/demo/csvupload/
├── CsvUploadApplication.java          ← @SpringBootApplication + @EnableAsync
├── config/
│   └── AsyncConfig.java               ← Thread-pool config for CSV workers
├── controller/
│   ├── UploadController.java          ← Upload form GET/POST + /progress page
│   └── JobController.java             ← REST /api/jobs/* for polling
├── model/
│   ├── Customer.java                  ← JPA entity
│   └── ProcessingJob.java             ← Job state with AtomicLong counters
├── repository/
│   └── CustomerRepository.java        ← Spring Data JPA
└── service/
    ├── CsvProcessingService.java      ← Core streaming + batch insert logic
    └── JobTrackerService.java         ← ConcurrentHashMap job store
```

---

## ⚡ Techniques Demonstrated

| # | Technique | Where |
|---|-----------|-------|
| 1 | **Streaming multipart upload** — temp file on disk, not in heap | `UploadController`, `application.properties` |
| 2 | **OpenCSV line-by-line streaming** — `BufferedReader` wrap | `CsvProcessingService.processCsv()` |
| 3 | **Hibernate JDBC batch inserts** — `batch_size=500`, `order_inserts=true` | `application.properties`, `flushBatch()` |
| 4 | **Async processing** — `@Async("csvProcessingExecutor")` | `CsvProcessingService.processAsync()` |
| 5 | **Per-batch `REQUIRES_NEW` transaction** — failure only rolls back current chunk | `CsvProcessingService.flushBatch()` |
| 6 | **Atomic progress counters** — `AtomicLong`, no locking between threads | `ProcessingJob` |
| 7 | **Progress polling** — UI polls `/api/jobs/{id}/status` every 2 s | `result.html` JS, `JobController` |
| 8 | **Row-level error collection** — bad rows captured, processing continues | `ProcessingJob.addError()` |
| 9 | **Client-side upload progress** — `XMLHttpRequest.upload.progress` event | `upload.html` JS |

---

## 📊 Expected CSV Format

```csv
externalId,firstName,lastName,email,phone,city,country,registrationDate
1001,John,Doe,john@example.com,555-1234,New York,US,2024-01-15
```

**Validation rules:**
- `firstName` and `lastName` are required
- `registrationDate` must be `yyyy-MM-dd` format (or blank)
- All other fields are optional

---

## 🧪 Generate Test Data

```bash
# Generate 100,000 rows (~8 MB)
python generate_customers.py --rows 100000 --output customers_100k.csv

# Generate 1,000,000 rows (~80 MB)
python generate_customers.py --rows 1000000 --output customers_1m.csv
```

---

## ⚙️ Configuration

`application.properties` key settings:

| Property | Default | Purpose |
|----------|---------|---------|
| `csv.processing.batch-size` | `500` | Rows per JDBC batch |
| `csv.processing.thread-pool-size` | `4` | Async worker threads |
| `spring.servlet.multipart.max-file-size` | `500MB` | Max upload size |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | `500` | Hibernate batch size |
| `spring.datasource.url` | `jdbc:sqlite:./data/customers.db` | SQLite file path |

---

## 🔗 API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/` | Upload form |
| `POST` | `/upload` | Accept CSV file, return redirect |
| `GET`  | `/progress/{jobId}` | Progress page |
| `GET`  | `/api/jobs/{jobId}/status` | JSON job status |
| `GET`  | `/api/jobs` | All jobs JSON |
| `GET`  | `/api/jobs/db-stats` | Total customer count in DB |

