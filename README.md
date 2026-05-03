# Professional Helium10 + Spring Integration + Spring Batch + Qwen Turbo

## Features
- Detect CSV files dropped in `input`
- Move file to `processing`
- Launch Spring Batch automatically
- Read Helium10 CSV format
- Normalize numeric fields like `"7,212"`
- Local scoring before AI
- Call Alibaba Cloud Qwen Turbo
- Retry on transient API failures
- Write result CSV to `output`
- Move processed source file to `archive`
- Move failed source file to `error`

## Required folders
- `C:/projet/input`
- `C:/projet/processing`
- `C:/projet/output`
- `C:/projet/archive`
- `C:/projet/error`

## Java version
Use **Java 17**.

## API key
Set environment variable:

```bash
DASHSCOPE_API_KEY=your_key_here
```

## Run
```bash
mvn spring-boot:run
```

## Output columns
```csv
keyword,localScore,aiScore,decision,status,explanation
```
