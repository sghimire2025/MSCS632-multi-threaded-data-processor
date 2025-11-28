# Data Processing System (Java + Gradle)

## Student: Suresh Ghimire

## 1. Project Overview

This project implements a **multi-threaded Data Processing System** in Java.  
Multiple worker threads retrieve tasks from a shared queue, process them, and write
results to a shared output resource (`processing_results.txt`).

The project demonstrates:

- Safe sharing of data structures between threads using synchronization.
- A producer–consumer pattern with a shared queue.
- Graceful thread termination using a *poison pill* task.
- Robust exception handling for thread interruptions and file I/O.
- Logging using Java's built-in `java.util.logging` framework.

This aligns with course requirements for concurrency management, exception handling,
and logging in a data processing application.

---

## 2. Technologies Used

- **Language:** Java (17 or higher recommended)
- **Build Tool:** Gradle
- **Concurrency:** Java threads, `synchronized`, `wait()`, `notifyAll()`
- **Logging:** `java.util.logging`
- **Collections:** `LinkedList`, `Collections.synchronizedList`

---

## 3. Project Structure

```text
data-processing-system/
├─ build.gradle
├─ settings.gradle
└─ src/
   └─ main/
      └─ java/
         ├─ model/DataTask.java
         ├─ TaskQueue.java
         ├─ Worker.java
         └─ DataProcessingApp.java
```



## 3. Getting Started

### 3.1 Prerequisites

- Java 17+ installed (`java -version`)
- Gradle installed, or use the Gradle wrapper if included
- A terminal / command prompt

### 3.2 Build and Run

From the project root (`data-processing-system/`), run:

```bash
# Using local Gradle
gradle run

# OR, using Gradle wrapper (if present)
./gradlew run        # macOS / Linux
gradlew.bat run      # Windows
```

On successful execution you should see:

- Log messages in the console showing workers starting, processing tasks,
  handling poison pills, and stopping.
- A file named `processing_results.txt` generated in the project root containing
  one line per processed task.

Example line in `processing_results.txt`:

```text
worker=2, taskId=5, payload=PAYLOAD-5, durationMs=231
```

---

## 4. Configuration

You can easily change the number of workers or tasks by editing constants in
`DataProcessingApp`:

```java
int numberOfWorkers = 4;
int numberOfTasks = 20;
```

Rebuild and run the application to see how concurrency behavior changes with
more or fewer threads.

---

## 5. Error Handling and Logging

- **Worker exceptions:** Any runtime errors during task processing are caught
  and logged with level `SEVERE`. The worker continues to process other tasks.
- **InterruptedException:** When retrieved from `TaskQueue` or `join()`, the
  interrupt status is restored with `Thread.currentThread().interrupt()` and a
  warning is logged.
- **I/O errors:** `IOException` during writing of `processing_results.txt` is
  caught in `DataProcessingApp` and logged with an explanatory message.

Logging is configured in the `configureLogging()` method using
`java.util.logging` and a `ConsoleHandler`.

---




