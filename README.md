# JSON Database Java 🗄️

A simple **Java console application** that simulates a basic JSON-based database.  
This project is a hands-on exercise in **object-oriented programming, file handling, and data management**.

Graduate Project from the **Hyperskill Java Track**.

## Features
- Store data in JSON format
- Create, read, update, and delete (CRUD) records
- Simple console-based interface
- Basic error handling for invalid inputs

## Technologies Used
- Java SE
- Object-Oriented Programming (OOP)
- Console I/O
- JSON file handling

## How to Run

1. Clone the repository:
```bash
git clone https://github.com/Dare2510/JSON-Database-Java.git
```
2. Navigate to the project folder:
```bash
cd JSON-Database-Java
```
3. Compile the Java files:
```bash
javac *.java
```
4. Run the application:
```bash
java Main [optional-input-file.json]

### Program Arguments / Usage
The application uses **command-line arguments** to perform operations on the database.

- Example to set a value:
```bash
java Main -t set -k text -v "Hello World!"
```
```bash
Project Structure
JSON-Database-Java/
│
├── Main.java         # Entry point of the application
├── Database.java     # Core logic for managing JSON data
├── Record.java       # Represents a single database record
└── README.md         # This file
```

Learning Goals

Practice Java basics: loops, conditions, and user input

Implement object-oriented concepts: classes, objects, encapsulation

Handle CRUD operations with JSON files

Learn basic file handling and data persistence
