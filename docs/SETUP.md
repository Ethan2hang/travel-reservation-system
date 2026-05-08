# Setup (Windows)

You're starting from zero: no MySQL, no Java project. Follow this once. The whole thing should take ~20 minutes.

## 1. Install MySQL Community Server 8.x

**Option A — official installer (recommended):**

1. Download MySQL Installer from https://dev.mysql.com/downloads/installer/
2. Run it → choose "Custom" → select **MySQL Server 8.x** and **MySQL Workbench** (optional but nice).
3. Configure:
   - Type: "Development Computer"
   - Authentication: **"Use Strong Password Encryption"**
   - Root password: pick something simple (e.g. `cs336root`). Write it down.
   - Windows Service: **leave checked**, name `MySQL80`.
   - Apply → Finish.
4. Verify: open Command Prompt and run
   ```
   mysql -u root -p
   ```
   Enter password. You should see the `mysql>` prompt. `\q` to exit.

**Option B — Docker (fallback, if installer fails):**

```powershell
docker run --name cs336mysql -e MYSQL_ROOT_PASSWORD=cs336root -p 3306:3306 -d mysql:8.4
```

## 2. Load the schema

From the project root (`C:\Assignments\CS336GroupProject`):

```powershell
mysql -u root -p < db\schema.sql
```

Verify:

```powershell
mysql -u root -p -e "USE travelres; SHOW TABLES; SELECT * FROM Employee;"
```

You should see 12 tables and one row in Employee (the seeded admin).

## 3. Install Java 21 (LTS) and Maven

- Java: https://adoptium.net/ → Temurin 21 LTS → Windows .msi installer.
  - Verify: `java -version` shows `21.x`.
- Maven: https://maven.apache.org/download.cgi → binary zip.
  - Extract to `C:\Tools\apache-maven-3.9.x`.
  - Add `C:\Tools\apache-maven-3.9.x\bin` to PATH.
  - Verify: `mvn -version`.

## 4. Project skeleton

The harness already wrote `pom.xml` and a starter `Db.java`. From the project root:

```powershell
mvn compile
```

Expected: BUILD SUCCESS, no errors. If you see "cannot find symbol" errors, you haven't pulled the JDBC driver yet — `mvn compile` should have done that. Re-run with `mvn -U compile` to force.

## 5. Smoke test the JDBC connection

```powershell
mvn exec:java -Dexec.mainClass=cs336.travel.Db
```

Expected output:

```
Connected to travelres at jdbc:mysql://localhost:3306/travelres
Found 1 employee row(s).
```

If this fails:

| Symptom | Fix |
|---|---|
| `Communications link failure` | MySQL service not running. `services.msc` → start MySQL80. |
| `Access denied for user 'root'@'localhost'` | Wrong password in `Db.java`. Edit `PASSWORD` constant. |
| `Unknown database 'travelres'` | Schema didn't load. Re-run step 2. |
| `Public Key Retrieval is not allowed` | Add `?allowPublicKeyRetrieval=true&useSSL=false` to JDBC URL. |

## 6. You're ready

Open the project in IntelliJ or VS Code, confirm `mvn compile` is green, then run the application with `mvn exec:java`.

## Connection params (default)

```
JDBC URL : jdbc:mysql://localhost:3306/travelres
User     : root
Password : (whatever you set in step 1)
Driver   : com.mysql.cj.jdbc.Driver
```

These live in `cs336.travel.Db` — edit there, not in random places.

## Windows path quirks

- If `mysql` isn't recognized after install, find it under `C:\Program Files\MySQL\MySQL Server 8.x\bin` and either add to PATH or use the full path.
- **PowerShell 7 does NOT support `<` for input redirection** ("reserved for future use"). To run a `.sql` file from PowerShell, use one of:
  ```powershell
  Get-Content db\schema.sql | mysql -u root -p
  mysql -u root -p -e "source db/schema.sql"
  ```
  Or run from `cmd.exe` instead, where `mysql -u root -p < db\schema.sql` works as written.
