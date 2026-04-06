# 🏥 MediScan

> Upload a medicine photo → AI identifies it → compare prices across platforms → see generic alternatives.

MediScan is a full-stack Spring Boot monolithic application designed to help users identify medicines from images using OCR and AI, compare their prices across various online pharmacy platforms, and discover cheaper bioequivalent generic alternatives.

> **Note:** The frontend application for this project is located in a separate repository. You can find it here: [MediscanFullstack (Frontend)](https://github.com/satyamkumarmishra2005/MediscanFullstack)

## 🚀 Key Features

*   **Medicine Image Upload**: Users can upload images of their medicine labels (via drag & drop or camera capture).
*   **AI-powered Medicine Identification**: Uses OCR.space to extract text and Gemini 2.0 Flash API to intelligently parse out the brand name, salt/composition, dosage, and manufacturer.
*   **Price Comparison**: Scrapes or fetches real-time prices from popular pharmacy platforms (like 1mg, Netmeds, PharmEasy) to find the best deals.
*   **Generic Alternatives**: Suggests cheaper, bioequivalent generic drugs based on the identified salt composition.
*   **Performance Tracking**: Leverages Redis caching to store scraped prices and minimize redundant processing, speeding up response times.

## 🛠️ Tech Stack

*   **Backend framework**: Spring Boot 4.x (Java 21)
*   **Build Tool**: Maven
*   **Database**: PostgreSQL
*   **Caching**: Redis
*   **AI/OCR Integration**: OCR.space REST API, Gemini 2.0 Flash API
*   **Scraping**: Jsoup web scraping
*   **Frontend**: React + TailwindCSS (Available in [MediscanFullstack](https://github.com/satyamkumarmishra2005/MediscanFullstack))

## 📐 System Architecture

MediScan uses a **Monolithic Architecture**. The core application relies on specialized service layers within a single Spring Boot runtime:
1.  **OCR Service Layer**: Calls OCR.space REST API with the uploaded image.
2.  **Medicine Parser Layer**: Passes the extracted raw text to the Gemini API with structured prompts to identify the exact medicine.
3.  **Price Aggregator Layer**: Checks the Redis cache for existing prices. If not found, scrapes pharmacy platforms, stores the result in PostgreSQL, and caches it in Redis.
4.  **Generic Finder Layer**: Queries the PostgreSQL database by salt/composition to return cheaper generic options.

## 🗄️ Core Database Schema

The system relies on PostgreSQL for persistence, encompassing:
*   `medicines`: Master table for identified drugs (brand, salt composition, dosage).
*   `medicine_prices`: Regularly updated pricing information across platforms.
*   `generic_alternatives`: Mapping of generic substitutes to specific branded medicines.

## 🔌 API Endpoints

*   `POST /api/v1/medicine/identify` - Upload an image to get medicine details.
*   `GET /api/v1/prices/{medicineId}` - Get prices across platforms for a specific medicine.
*   `GET /api/v1/generics?salt={salt}` - Look up generic alternatives by salt composition.
*   `GET /api/v1/medicine/search?q={name}` - Textual search fallback.

## ⚙️ Getting Started

### Prerequisites
*   Java 21 or higher
*   Maven 3.8+
*   PostgreSQL running locally or on a remote server
*   Redis server
*   API Keys for **OCR.space** and **Gemini 2.0 Flash** 

### Installation
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/satyamkumarmishra2005/Mediscan.git
    cd Mediscan
    ```
2.  **Configure environment variables:**
    Update `src/main/resources/application.properties` with your database credentials, Redis configuration, and API keys.
3.  **Build and run the application:**
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

## 📜 License
This project is for educational and portfolio purposes.
