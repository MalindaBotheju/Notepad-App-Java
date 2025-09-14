# Notepad App – Java Swing

## Overview
This is a **Notepad application** built using **Java Swing**.  
It provides basic text editing features along with support for **recently opened files stored in a SQLite database**.

> Developed for the *Advanced Programming Techniques* assignment.

## Features
- Create new documents
- Open existing `.txt` files
- Save text files
- Recently opened files (stored in **SQLite database** `notepad.db`)
- Cut / Copy / Paste
- Find & Replace
- Change font and text color
- Status bar showing cursor position (line & column)

## Setup Instructions

### Requirements
- **Java Version:** Java 8 or later  
- **SQLite JDBC Driver:** Add the [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) JAR file to your classpath.

### Database Setup
- No manual setup is required.  
- On the first run, the app will automatically create a file named **`notepad.db`** in the project folder.  
- Inside it, a table named `recent_files` is created:

```sql
CREATE TABLE IF NOT EXISTS recent_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT NOT NULL UNIQUE,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
