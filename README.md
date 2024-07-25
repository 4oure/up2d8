# up2d8: Effortlessly Sync Your Canvas Creations

[![Build Status](https://github.com/4oure/up2d8/actions/workflows/build.yml/badge.svg)](https://github.com/4oure/up2d8/actions)
[![Maintainability](https://api.codeclimate.com/v1/repos/4oure/up2d8/badges/maintainability)](https://api.codeclimate.com/v1/repos/4oure/up2d8/maintainability)
[![Coverage](https://coveralls.io/repos/github/4oure/up2d8/badge.svg?branch=master)](https://coveralls.io/github/4oure/up2d8)

**up2d8** streamlines your canvas workflow by automatically syncing your creations across devices.   ↔️ ☁️

## What is up2d8?

up2d8 is a Java-based file synchronization service specifically designed for canvas applications. It ensures your masterpieces are always within reach, no matter where inspiration strikes.  

## Features

* **Effortless Synchronization:** Set it up once, and up2d8 seamlessly synchronizes your canvas files in the background, keeping them up-to-date across all your devices.
* **Platform Independence:** Work freely on any device – up2d8 transcends operating systems, ensuring your creations are readily accessible.
* **Secure Storage:** Rest assured, your precious artwork is securely stored in your chosen cloud storage provider (e.g., Dropbox, Google Drive).
* **Lightweight Footprint:** up2d8 operates efficiently without bogging down your system resources, allowing you to focus on your artistic endeavors.

## Getting Started

1. **Prerequisites:**
   * Java 11+
   * A supported cloud storage provider account (e.g., Dropbox, Google Drive)
   * A Canvas API token

2. **Installation:**

   There are two ways to get started with up2d8:

   * **Clone the repository:**

     ```bash
     git clone [https://github.com/4oure/up2d8.git](https://github.com/your-username/up2d8.git)
     ```

3. **Configuration:**

   * Edit the `config.json` file with your Canvas API token and other necessary configuration options.
   * Refer to the `config.json` file for the expected format.
  
       ```config.json
       { "canvas_api_key": "your-api-key-here" }
     ```

4. **Run up2d8:**

   Navigate to the project directory and run the JAR file:

   ```bash
   java -jar up2d8.jar
