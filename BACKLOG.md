# Backup of PostgreSQL Schemas with HenPlus

## Overview
A Bash script (`backup-schema.sh`) was created to back up **every table** from all user databases on the remote PostgreSQL server `postgres.linuxtampa.com` using the `henplus` binary.

### Key Features
- Reads the password from `~/.pgpass` (or optional `DB_PASS`).
- Retrieves the list of databases (excluding templates) and then enumerates all tables (`schema.table`).
- Dumps each table with `dump-out`, storing the result as:
  ```
  ./local-postgres.linuxtampa.com/schema/<database>/<schema>.<table>.txt
  ```
- Uses a **silent mode** (`-s`) for HenPlus to suppress noisy warnings like *"Unable to create a system terminal"*.
- Verifies that each dump file is created and non‑empty; on failure the script exits with a clear error message and a non‑zero exit code.
- The script aborts on any command error (`set -euo pipefail`).

## Usage
```bash
chmod +x backup-schema.sh   # make executable (already done)
./backup-schema.sh          # runs the full backup
```
Make sure `~/.pgpass` contains an entry for each database, e.g.:
```
postgres.linuxtampa.com:5432:*:postgres:<your‑password>
```
or set the environment variable `DB_PASS` before invoking the script.

## Outcome
Running the script produced **hundreds of dump files**, one per table, organized by database and schema. All previously observed warnings have been eliminated, and any failure now aborts the process with a meaningful error message.

---
*Generated automatically by an AI‑assisted coding session.*
