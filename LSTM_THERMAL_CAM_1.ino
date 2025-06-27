/***************************************************************************
LSTM Thermal Cam with Touchscreen
 ***************************************************************************/
#include <SPI.h>
#include <TFT_eSPI.h>
#include <Wire.h>
#include <Adafruit_AMG88xx.h>
#include <XPT2046_Touchscreen.h>
#include <DHT.h> 
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <ArduinoJson.h>

#define WIFI_SSID "SKYW_8B18_2G"
#define WIFI_PASS "3NFddeWt"   

#define API_KEY "AIzaSyCcMUTV5GeIBpB4Fkd6DEWoDDX1QTGBUvc"
#define FIREBASE_PROJECT_ID "lstm-d75ee"
#define USER_EMAIL "lstm@gmail.com"
#define USER_PASSWORD "21void"

// Firebase Realtime Database URL
#define DATABASE_URL "https://lstm-d75ee-default-rtdb.asia-southeast1.firebasedatabase.app"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

String dhtTempPath = "/DHTTemp/currentTemp";
String dhtHumidPath = "/DHTHumid/currentHumid";

#define DHTPIN 5     
#define DHTTYPE DHT22   
DHT dht(DHTPIN, DHTTYPE);

#define XPT2046_IRQ 36   // T_IRQ
#define XPT2046_MOSI 32  // T_DIN
#define XPT2046_MISO 39  // T_OUT
#define XPT2046_CLK 25   // T_CLK
#define XPT2046_CS 33    // T_CS

#define STMPE_CS 33
#define SD_CS    2
#define SDA_PIN 16
#define SCL_PIN 17

// Low range of the sensor (this will be blue on the screen)
#define MINTEMP 20

// High range of the sensor (this will be red on the screen)
#define MAXTEMP 37

const uint16_t camColors[] = {0x480F,
0x400F,0x400F,0x400F,0x4010,0x3810,0x3810,0x3810,0x3810,0x3010,0x3010,
0x3010,0x2810,0x2810,0x2810,0x2810,0x2010,0x2010,0x2010,0x1810,0x1810,
0x1811,0x1811,0x1011,0x1011,0x1011,0x0811,0x0811,0x0811,0x0011,0x0011,
0x0011,0x0011,0x0011,0x0031,0x0031,0x0051,0x0072,0x0072,0x0092,0x00B2,
0x00B2,0x00D2,0x00F2,0x00F2,0x0112,0x0132,0x0152,0x0152,0x0172,0x0192,
0x0192,0x01B2,0x01D2,0x01F3,0x01F3,0x0213,0x0233,0x0253,0x0253,0x0273,
0x0293,0x02B3,0x02D3,0x02D3,0x02F3,0x0313,0x0333,0x0333,0x0353,0x0373,
0x0394,0x03B4,0x03D4,0x03D4,0x03F4,0x0414,0x0434,0x0454,0x0474,0x0474,
0x0494,0x04B4,0x04D4,0x04F4,0x0514,0x0534,0x0534,0x0554,0x0554,0x0574,
0x0574,0x0573,0x0573,0x0573,0x0572,0x0572,0x0572,0x0571,0x0591,0x0591,
0x0590,0x0590,0x058F,0x058F,0x058F,0x058E,0x05AE,0x05AE,0x05AD,0x05AD,
0x05AD,0x05AC,0x05AC,0x05AB,0x05CB,0x05CB,0x05CA,0x05CA,0x05CA,0x05C9,
0x05C9,0x05C8,0x05E8,0x05E8,0x05E7,0x05E7,0x05E6,0x05E6,0x05E6,0x05E5,
0x05E5,0x0604,0x0604,0x0604,0x0603,0x0603,0x0602,0x0602,0x0601,0x0621,
0x0621,0x0620,0x0620,0x0620,0x0620,0x0E20,0x0E20,0x0E40,0x1640,0x1640,
0x1E40,0x1E40,0x2640,0x2640,0x2E40,0x2E60,0x3660,0x3660,0x3E60,0x3E60,
0x3E60,0x4660,0x4660,0x4E60,0x4E80,0x5680,0x5680,0x5E80,0x5E80,0x6680,
0x6680,0x6E80,0x6EA0,0x76A0,0x76A0,0x7EA0,0x7EA0,0x86A0,0x86A0,0x8EA0,
0x8EC0,0x96C0,0x96C0,0x9EC0,0x9EC0,0xA6C0,0xAEC0,0xAEC0,0xB6E0,0xB6E0,
0xBEE0,0xBEE0,0xC6E0,0xC6E0,0xCEE0,0xCEE0,0xD6E0,0xD700,0xDF00,0xDEE0,
0xDEC0,0xDEA0,0xDE80,0xDE80,0xE660,0xE640,0xE620,0xE600,0xE5E0,0xE5C0,
0xE5A0,0xE580,0xE560,0xE540,0xE520,0xE500,0xE4E0,0xE4C0,0xE4A0,0xE480,
0xE460,0xEC40,0xEC20,0xEC00,0xEBE0,0xEBC0,0xEBA0,0xEB80,0xEB60,0xEB40,
0xEB20,0xEB00,0xEAE0,0xEAC0,0xEAA0,0xEA80,0xEA60,0xEA40,0xF220,0xF200,
0xF1E0,0xF1C0,0xF1A0,0xF180,0xF160,0xF140,0xF100,0xF0E0,0xF0C0,0xF0A0,
0xF080,0xF060,0xF040,0xF020,0xF800,};

TFT_eSPI tft = TFT_eSPI(); // Create TFT instance
Adafruit_AMG88xx amg;
XPT2046_Touchscreen touchscreen(XPT2046_CS, XPT2046_IRQ);

unsigned long delayTime;
#define AMG_COLS 8
#define AMG_ROWS 8
float pixels[AMG_COLS * AMG_ROWS];
#define INTERPOLATED_COLS 40
#define INTERPOLATED_ROWS 40

#define RELAY_PIN 27 // Pin 27 for the relay
#define RELAY_ON HIGH // Relay is activated when set to HIGH
#define RELAY_OFF LOW

// Function prototypes
float get_point(float *p, uint8_t rows, uint8_t cols, int8_t x, int8_t y);
void interpolate_image(float *src, uint8_t src_rows, uint8_t src_cols, float *dest, uint8_t dest_rows, uint8_t dest_cols);
void drawpixels(float *p, uint8_t rows, uint8_t cols, uint16_t boxWidth, uint16_t boxHeight);
void displayMaxTemperature(float *pixels);
void drawButton();
void drawStopButton();
void checkTouch();
void clearStopButton();

bool capturing = false; // Flag for capturing state

// New button properties
#define STOP_BUTTON_WIDTH 80
#define STOP_BUTTON_HEIGHT 40

int stopButtonX, stopButtonY;

// Firebase update interval variables
unsigned long lastFirebaseUpdate = 0;
const unsigned long firebaseInterval = 5000;  // Update every 5 seconds

void setup() {
    Serial.begin(115200);

    WiFi.begin(WIFI_SSID, WIFI_PASS);
    Serial.print("Connecting to Wi-Fi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());

    Wire.begin(SDA_PIN, SCL_PIN);
    SPI.begin(XPT2046_CLK, XPT2046_MISO, XPT2046_MOSI, XPT2046_CS);
    touchscreen.begin();

    // Set touchscreen rotation and TFT display
    touchscreen.setRotation(1);
    tft.init();
    tft.setRotation(1);
    tft.fillScreen(TFT_BLACK);

    // Initialize AMG sensor
    if (!amg.begin()) {
        Serial.println("Could not find a valid AMG88xx sensor, check wiring!");
        while (1) { delay(1); }
    }

    dht.begin(); 
    setupRelay();

    Serial.println("-- Thermal Camera Test --");
    drawButton(); // Draw the start button
    config.api_key = API_KEY;
    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;
    config.database_url = DATABASE_URL; // Set the Realtime Database URL
    config.token_status_callback = tokenStatusCallback;  // Token callback to handle auth tokens
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
}

void loop() {
    checkTouch(); // Check for touch input

    // Read DHT temperature and humidity
    float dhtTemp = dht.readTemperature();  
    float humidity = dht.readHumidity();   

    if (capturing) {
        Serial.print("DHT Temperature = ");
        Serial.print(dhtTemp);
        Serial.println(" *C");
        

        Serial.print("Humidity = ");
        Serial.print(humidity);
        Serial.println(" %");

        controlRelay(dhtTemp);

        // Read all the pixels from the AMG sensor
        amg.readPixels(pixels);
        Serial.print("[");
        for (int i = 0; i < AMG_COLS * AMG_ROWS; i++) {
            Serial.print(pixels[i]);
            Serial.print(", ");
            if ((i + 1) % AMG_COLS == 0) Serial.println();
        }
        Serial.println("]");
        
        float dest_2d[INTERPOLATED_ROWS * INTERPOLATED_COLS];
        int32_t t = millis();
        interpolate_image(pixels, AMG_ROWS, AMG_COLS, dest_2d, INTERPOLATED_ROWS, INTERPOLATED_COLS);
        Serial.print("Interpolation took "); Serial.print(millis() - t); Serial.println(" ms");

        uint16_t boxWidth = tft.width() / INTERPOLATED_COLS;
        uint16_t boxHeight = tft.height() / INTERPOLATED_ROWS;

        drawpixels(dest_2d, INTERPOLATED_ROWS, INTERPOLATED_COLS, boxWidth, boxHeight);
        
        // Display the max temperature and DHT data
        displayMaxTemperature(pixels, dhtTemp, humidity);
    }
}

void setupRelay() {
    pinMode(RELAY_PIN, OUTPUT); // Set the relay pin as output
    digitalWrite(RELAY_PIN, RELAY_OFF); // Ensure the relay is off initially
}

void controlRelay(float temp) {
    if (temp >= 35) {
        digitalWrite(RELAY_PIN, RELAY_ON);  // Turn relay ON if temperature >= 35°C
        Serial.println("Relay ON - Temperature above 35°C");
    } else if (temp <= 32) {
        digitalWrite(RELAY_PIN, RELAY_OFF); // Turn relay OFF if temperature <= 31°C
        Serial.println("Relay OFF - Temperature below 31°C");
    }
}

void drawpixels(float *p, uint8_t rows, uint8_t cols, uint16_t boxWidth, uint16_t boxHeight) {
    int colorTemp;
    for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
            float val = p[y * cols + x]; // Access pixel value directly
            if (val >= MAXTEMP) colorTemp = MAXTEMP;
            else if (val <= MINTEMP) colorTemp = MINTEMP;
            else colorTemp = val;

            uint8_t colorIndex = map(colorTemp, MINTEMP, MAXTEMP, 0, 255);
            colorIndex = constrain(colorIndex, 0, 255);

            // Draw the pixels to fill the screen
            tft.fillRect(boxWidth * x, boxHeight * y, boxWidth, boxHeight, camColors[colorIndex]);
        }
    }
}

void displayMaxTemperature(float *pixels, float dhtTemp, float humidity) {
    // Get the maximum temperature from the AMG sensor
    float maxTemp = pixels[0];
    for (int i = 1; i < AMG_COLS * AMG_ROWS; i++) {
        if (pixels[i] > maxTemp) {
            maxTemp = pixels[i];
        }
    }

    // Display the maximum temperature in the top right corner
    tft.setTextColor(TFT_WHITE);
    tft.setTextSize(1);
    tft.setCursor(tft.width(), 5); // Adjust cursor position for max temperature display
    tft.print("Max Temp: ");
    tft.print(maxTemp, 1);
    tft.print(" C");
}

void drawButton() {
    // Set the text size and color first
    tft.setTextSize(2);
    tft.setTextColor(TFT_WHITE);

    // Get the width of the text
    int textWidth = tft.textWidth("Start");
    // Get the height of the text for the specified size
    int textHeight = tft.fontHeight(2); 

    // Calculate the button's position and size
    int buttonWidth = textWidth + 20; // Add some padding
    int buttonHeight = textHeight + 5; // Reduce padding to minimize extra space
    int x = (tft.width() / 2) - (buttonWidth / 2); // Center x
    int y = (tft.height() / 2) - (buttonHeight / 2); // Center y

    // Draw the button background
    tft.fillRect(x, y, buttonWidth, buttonHeight, TFT_BLUE);

    // Set cursor position for text
    tft.setCursor(x + (buttonWidth / 2) - (textWidth / 2), y + (buttonHeight / 2) - (textHeight / 2) + 2); // Adjust vertical position slightly
    tft.print("Start");
}

void clearStopButton() {
    // Clear the stop button area
    tft.fillRect(stopButtonX, stopButtonY, STOP_BUTTON_WIDTH, STOP_BUTTON_HEIGHT, TFT_BLACK);
}

void checkTouch() {
    if (touchscreen.tirqTouched() && touchscreen.touched()) {
        TS_Point p = touchscreen.getPoint();
        int x = map(p.x, 200, 3700, 0, tft.width());
        int y = map(p.y, 240, 3800, 0, tft.height());

        // Check if touch is within the start button area
        if (x >= (tft.width() / 2) - 60 && x <= (tft.width() / 2) + 60 &&
            y >= (tft.height() / 2) - 15 && y <= (tft.height() / 2) + 15) {
            if (!capturing) {
                capturing = true; // Start capturing on touch
                tft.fillScreen(TFT_BLACK); // Clear the screen for the thermal display
            }
        }
    }
}

void saveDataToRealtimeDatabase(const float &data, const String &nodePath) {
    // Update data in Realtime Database
    Serial.print("Updating data in Realtime Database (Path: ");
    Serial.print(nodePath);
    Serial.println(")... ");
    if (Firebase.RTDB.set(&fbdo, nodePath.c_str(), data)) {
        Serial.println("Success");
    } else {
        Serial.println(fbdo.errorReason());
    }
}