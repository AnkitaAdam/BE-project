# Vulnuris RCA Platform – Requirements

## 1. Problem Statement
Build a system that:
- Ingests logs
- Normalizes into common schema
- Correlates events
- Generates timeline + RCA report

## 2. Core Features

### Ingestion
- CSV, JSONL, syslog
- Streaming support
- Metadata capture

### Normalization
- Convert to Common Event Schema (CES)
- Normalize timestamps to UTC
- Preserve raw reference

### Correlation
- Directed Event Graph
- Link via user, IP, host, object
- Confidence scoring

### RCA Report
- Root cause
- Attack path
- Impact
- IOCs
- Recommendations
- Export PDF/HTML

## 3. CES Fields
- @ts_utc
- @ts_original
- source_type
- user
- src_ip
- dst_ip
- action
- object
- result
- message
- raw_ref

## 4. API Contracts
POST /ingest
GET /timeline
GET /graph
POST /report

## 5. Non-Functional
- 100k events < 5 min
- <2GB memory
- Docker runnable
