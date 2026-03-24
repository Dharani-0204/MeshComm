# Firebase & Google Maps Setup Instructions

## Prerequisites
- A Google Cloud Platform account
- A Firebase project

## Step 1: Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select an existing one
3. Add an Android app with package name: `com.meshcomm`
4. Download the `google-services.json` file
5. Place it in `app/` directory

### Required Firebase Services:
- **Firestore Database**: For storing SOS alerts and user profiles
- **Authentication** (optional): For user authentication
- **Cloud Messaging** (optional): For push notifications

### Firestore Security Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User profiles - civilians can read/write their own, rescuers can read all
    match /user_profiles/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // SOS alerts - anyone authenticated can create, rescuers can read all
    match /sos_alerts/{alertId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
    }

    // Active devices - authenticated users can read/write their own
    match /active_devices/{deviceId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

## Step 2: Google Maps Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable the following APIs:
   - Maps SDK for Android
   - Maps JavaScript API (if needed)
3. Create an API key with Android app restriction
4. Add your app's SHA-1 fingerprint
5. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` in `app/src/main/res/values/strings.xml`

### Getting SHA-1 Fingerprint:

```bash
# For debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# For Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

## Step 3: Create google-services.json

Your `google-services.json` should be placed in the `app/` directory. It will look something like this:

```json
{
  "project_info": {
    "project_number": "YOUR_PROJECT_NUMBER",
    "project_id": "YOUR_PROJECT_ID",
    "storage_bucket": "YOUR_PROJECT_ID.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "YOUR_APP_ID",
        "android_client_info": {
          "package_name": "com.meshcomm"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "YOUR_API_KEY"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

## File Structure After Setup

```
app/
├── google-services.json     <-- Add this file
├── src/
│   └── main/
│       └── res/
│           └── values/
│               └── strings.xml  <-- Update google_maps_key here
```

## Testing

1. Build the app: `./gradlew assembleDebug`
2. Install on device
3. Select "Rescuer" role to access the dashboard
4. Test SOS alerts from another device with "Civilian" role

## Troubleshooting

### Firebase Issues:
- Check if `google-services.json` is in the correct location
- Verify package name matches in Firebase Console
- Check logcat for Firebase initialization errors

### Google Maps Issues:
- Verify API key is not restricted incorrectly
- Check if SHA-1 fingerprint is added to Google Cloud Console
- Ensure Maps SDK for Android is enabled

### Build Issues:
- Run `./gradlew clean` before rebuilding
- Check for dependency conflicts in build.gradle
