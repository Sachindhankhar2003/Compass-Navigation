# CompassNav 🧭✨

A beautiful, high-performance, space-themed Android navigation application built with Jetpack Compose. Unlike traditional turn-by-turn map directions, **CompassNav** guides you using a live, custom-rendering celestial compass that points directly toward your destination in real time.

---

## How it Works

CompassNav operates in three main stages:

### 1. Destination Input (Home Screen)
* Enter any destination address or landmark (e.g., "Paris, France" or "Central Park, NY").
* The app queries the local Android `Geocoder` service asynchronously to resolve the street name into exact latitude and longitude coordinates.

### 2. Route Visualization (Map Screen)
* Displays an interactive map of your current position and destination connected by a neon cyan path.
* Supports toggleable map providers:
  - **OpenStreetMap** (via custom Leaflet WebView, no API configuration needed).
  - **Google Maps** (using the Native Android Google Maps SDK).
* Calculates real-time distance and estimated driving time based on average speed.

### 3. Compass Guidance (Navigation Screen)
* Displays a custom-drawn space-themed compass dial.
* The physical needle points directly to your destination regardless of which way you rotate or tilt your phone.
* Provides turn-by-turn arrow directions (Go Straight, Turn Left, Turn Right, Turn Around) based on the angle difference between your direction and the destination.
* Displays a congratulations confetti screen once you arrive within **50 meters** of your target.

---

## Under the Hood

### Sensor Integration
The application uses two hardware-level inputs combined mathematically to orient the compass needle:
1. **Compass Heading**: Uses the device's physical hardware sensors (`Sensor.TYPE_ACCELEROMETER` and `Sensor.TYPE_MAGNETIC_FIELD`) to determine the angle relative to magnetic North.
2. **GPS Location**: Uses Google Play Services `FusedLocationProviderClient` to get high-accuracy coordinate updates every second.

### Mathematical Calculations
* **Distance**: Computes the great-circle distance between two coordinates using the Haversine formula (`currentLocation.distanceTo(destination)`).
* **Bearing Angle**: Calculates the absolute angle from the user's current coordinate to the destination coordinate (`currentLocation.bearingTo(destination)`).
* **Needle Rotation**: The visual angle of the compass needle is calculated dynamically as:
  $$\text{Needle Angle} = \text{Bearing to Destination} - \text{Current Phone Heading}$$
* **Smooth Interpolation**: Utilizes Kotlin math helper filters to animate the needle rotations and prevent 360-degree wrapping/spinning when crossing the North pole boundary.
