# Subtitle Translator

This project translates subtitle files (SRT format) into Vietnamese using the Gemini API.

## Prerequisites

- Java 17 or higher
- Maven
- An API key for the Gemini API

## Setup

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/subtitle-translator.git
    cd subtitle-translator
    ```

2. Update the `API_KEY` in `src/main/java/SubtitleTranslator.java`:
    ```java
    private static final String API_KEY = "YOUR_API_KEY";
    ```

3. Build the project using Maven:
    ```sh
    mvn clean install
    ```

## Usage

1. Run the application:
    ```sh
    java -cp target/subtitle_translate-1.0-SNAPSHOT.jar SubtitleTranslator
    ```

2. Enter the input file path and output file path when prompted:
    ```
    Enter the input file path: /path/to/your/input.srt
    Enter the output file path: /path/to/your/output.srt
    ```

## Project Structure

- `src/main/java/SubtitleTranslator.java`: Main class for translating subtitles.
- `pom.xml`: Maven configuration file.

## Dependencies

- `org.apache.httpcomponents.client5:httpclient5:5.4.1`
- `org.apache.httpcomponents.client5:httpclient5-fluent:5.4.1`
- `com.fasterxml.jackson.core:jackson-databind:2.15.2`

## License

This project is licensed under the MIT License.