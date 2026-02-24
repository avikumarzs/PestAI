from flask import Flask, jsonify
import serial
import threading

app = Flask(__name__)

# Connected to COM3 based on your screenshot
try:
    ser = serial.Serial("COM3", 9600)
except Exception as e:
    print("Could not connect to COM3. Make sure the Arduino Serial Monitor is CLOSED.")

# Store the latest values here
sensor_data = {
    "temperature": "--",
    "humidity": "--",
    "ldr": "--",
    "distance": "--"
}

def read_from_port():
    while True:
        if ser.in_waiting > 0:
            line = ser.readline().decode('utf-8', errors='ignore').strip()
            # Parse the specific text from your Arduino
            if "Temp:" in line:
                sensor_data["temperature"] = line.split(":")[1].replace("C", "").strip()
            elif "Humidity:" in line:
                sensor_data["humidity"] = line.split(":")[1].replace("%", "").strip()
            elif "LDR Value:" in line:
                sensor_data["ldr"] = line.split(":")[1].strip()
            elif "Distance:" in line:
                sensor_data["distance"] = line.split(":")[1].replace("cm", "").strip()

# Run the serial reader in the background so it doesn't block the web server
threading.Thread(target=read_from_port, daemon=True).start()

@app.route("/data")
def get_data():
    return jsonify(sensor_data)

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)