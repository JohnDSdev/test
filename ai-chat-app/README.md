# AI Chat App for Android

This repository contains an Android application that runs a large‑language model (LLM) on device using the Cactus Compute SDK.  It provides a familiar chat interface, optional voice input/output, and exposes two local APIs over your Wi‑Fi network so that other devices on the same LAN can programmatically query the model.  Everything runs completely offline except when web search is enabled.

## Features

* **On‑device LLM** – Load a GGUF model and chat with it without any internet connection.  The Cactus Compute engine streams tokens for low latency.
* **Chat UI** – A Material 3 Jetpack Compose interface with streaming responses and a persistent transcript.
* **Voice mode** – Speech recognition for input and TextToSpeech for output (planned).
* **Tools** – Optional helpers that the model can call:
  * **Web Search** – Uses the Ollama Search API when provided with your API key to fetch and summarize web results.
  * **Time** – Returns the current time and timezone.
  * **Location** – Returns device latitude/longitude and optionally reverse geocodes it.  Only used when the Location tool is enabled in settings.
* **LAN APIs** – Two REST/WebSocket APIs served from your phone via Ktor:
  * **Normal API** – `POST /v1/chat` with optional Server Sent Events streaming; `POST /v1/speech` to invoke TextToSpeech.
  * **Weird API** – `GET /events` over WebSocket mirrors every user, assistant and tool event as JSON.
* **Settings** – Persistent controls for enabling/disabling APIs and tools, changing the server port, managing your Ollama API key, adjusting model sampling parameters, picking a model and copying your local API token.
* **Model management** – Search, download and activate GGUF models from Hugging Face (download logic is stubbed in this implementation).
* **GitHub CI** – Every push to `main` builds a debug APK and uploads it as a GitHub Actions artifact.  Tagging a version (e.g. `v0.1.0`) automatically creates a GitHub Release with the APK attached so you can download it from a stable URL.

## Sideloading the APK

Android only allows installation of apps from outside Google Play when you enable “Unknown Sources.”  To install the debug APK built by GitHub Actions:

1. Navigate to your repository on GitHub and select **Actions → Android CI (Debug APK) → Artifacts**.  Download the `app-debug-apk` artifact.
2. On your phone, go to **Settings → Security → Install unknown apps** and grant permission to the app (e.g. Chrome or Files) you will use to open the APK.
3. Transfer the APK to your phone and open it.  Follow the prompts to install.

Alternatively, after tagging a release (e.g. `git tag v0.1.0 && git push --tags`), go to **Releases** on GitHub.  The release artifact contains the APK with a permanent download link.

## Finding Your Phone’s LAN IP and API Token

To use the LAN APIs from another device:

1. Make sure your phone and the client device are on the same Wi‑Fi network.
2. Open **Settings** in the app and enable the **Normal API** and/or **Weird API**.  Note the **Server Port** setting (default `17890`).
3. The app displays your **Local API Token**.  Tap the copy icon to copy it to the clipboard.  This token must be sent as a Bearer token in the `Authorization` header of all API requests.
4. Determine your phone’s local IP address (e.g. via **Settings → About phone → Status** or by long‑pressing the connected Wi‑Fi network).  Compose the base URL as `http://<phone-ip>:<port>`.

## Entering Your Ollama Search API Key

If you wish to enable the web search tool, you must provide a valid Ollama Search API key:

1. Open **Settings → Enable Web Search Tool**.
2. Paste your API key into the **Ollama Search API Key** field and press **Save**.  The key is stored securely using Android’s `EncryptedSharedPreferences` and never leaves your device.
3. Use the **Enable Web Search Tool** toggle to turn the search tool on or off at any time.

Use the **Test** button (if implemented) to verify that your key is accepted by the search endpoint.

## API Usage Examples

All API requests require an `Authorization: Bearer <LOCAL_API_TOKEN>` header.  Replace `PHONE_LAN_IP` with the local IP of your phone and `PASTE_LOCAL_API_TOKEN` with the token from the settings screen.

### Python – Non‑Streaming `/v1/chat`

```python
import requests

BASE = "http://PHONE_LAN_IP:17890"
TOKEN = "PASTE_LOCAL_API_TOKEN"

body = {
  "messages": [{"role": "user", "content": "Give me a two‑sentence summary of entropy."}],
  "tools": [
    {"name": "search", "enabled": True},
    {"name": "time", "enabled": True}
  ],
  "stream": False,
  "params": {"temperature": 0.7, "top_p": 0.9, "top_k": 40, "max_tokens": 256, "context_window": 8192}
}

r = requests.post(f"{BASE}/v1/chat",
                  headers={"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"},
                  json=body,
                  timeout=600)
print(r.json())
```

### Python – Streaming `/v1/chat` (SSE)

```python
import requests
from sseclient import SSEClient

BASE = "http://PHONE_LAN_IP:17890"
TOKEN = "PASTE_LOCAL_API_TOKEN"

body = {
  "messages": [{"role": "user", "content": "Stream a haiku about magnets."}],
  "stream": True
}

r = requests.post(f"{BASE}/v1/chat",
                  headers={"Authorization": f"Bearer {TOKEN}", "Accept": "text/event-stream"},
                  json=body,
                  stream=True)

for event in SSEClient(r).events():
    print(event.event, event.data)
```

### Python – Weird API `/events` (WebSocket)

```python
import asyncio, json, websockets

BASE_WS = "ws://PHONE_LAN_IP:17890/events"
TOKEN = "PASTE_LOCAL_API_TOKEN"

async def main():
    async with websockets.connect(BASE_WS, extra_headers={"Authorization": f"Bearer {TOKEN}"}) as ws:
        async for msg in ws:
            print(json.loads(msg))

asyncio.run(main())
```

### MicroPython – Non‑Streaming `/v1/chat`

```python
import urequests as requests
import ujson as json

BASE = "http://PHONE_LAN_IP:17890"
TOKEN = "PASTE_LOCAL_API_TOKEN"

body = {
  "messages": [{"role": "user", "content": "Name three prime numbers under 20."}],
  "stream": False
}

r = requests.post(BASE + "/v1/chat",
    headers={"Authorization": "Bearer " + TOKEN, "Content-Type": "application/json"},
    data=json.dumps(body))
print(r.text)
r.close()
```

### MicroPython – Weird API `/events` (WebSocket)

```python
import ujson as json
import uwebsockets.client as wscl

BASE_WS = "ws://PHONE_LAN_IP:17890/events"
TOKEN = "PASTE_LOCAL_API_TOKEN"

ws = wscl.connect(BASE_WS, headers={"Authorization": "Bearer " + TOKEN})
try:
    while True:
        msg = ws.recv()
        if msg is None:
            break
        print(json.loads(msg))
finally:
    ws.close()
```

## Battery Optimization

The embedded server runs in a foreground service to avoid being killed by the system.  To ensure reliable operation when the phone is idle, you may need to exempt the app from battery optimizations:

1. Open **Settings → Apps → AI Chat → Battery** (exact path varies by OEM).
2. Select **Unrestricted** or **Don't optimize**.  Without this exemption the server may be paused when the device enters Doze mode.

## Privacy Notice

This app only accesses location data when the **Location Tool** is enabled in settings.  The latitude, longitude and optional place name are returned to the local model or API caller and are **not** sent to any remote server by the app.  You can disable the Location tool at any time, and you retain full control over your data.