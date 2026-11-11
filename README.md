# TvGoogle ("Google Chrome")

A minimal Google Search app for Android TV. It's a single `WebView` pointed at `google.com`, with a virtual mouse cursor overlaid so it's usable with a TV remote.

TV remotes have no touchscreen, and the stock Android `WebView` consumes D-pad keys itself for its own spatial-navigation logic — often failing to reach checkboxes or cross-origin iframes (e.g. Google's "are you a robot" challenge). This project intercepts D-pad keys before the `WebView` ever sees them, draws an on-screen cursor driven by those keys, and dispatches real touch (`MotionEvent`) events at the cursor's position — so to the WebView it looks exactly like a real finger/mouse.

## Features

- **Virtual cursor** — moves with the D-pad, accelerates while a direction is held (`CursorView.java`, PNG arrow icon, hotspot = the arrow's tip).
- **Click and drag** — the OK/center key produces a real `ACTION_DOWN` → (if moved) `ACTION_MOVE` → `ACTION_UP` sequence. A short press is a click; holding it down while moving the D-pad is a drag-scroll, for custom-scroll widgets that plain scrolling can't reach.
- **Edge scrolling** — pushing the cursor into a screen edge scrolls the page in that direction; gated by `WebView.canScrollVertically/Horizontally`, so there's no fake overscroll bounce and no dead zone when reversing direction.
- **Bot-detection-friendly setup** — no desktop User-Agent spoofing (the WebView uses its own real UA, so Client Hints stay consistent), persistent cookies/session via `CookieManager`.
- **Custom icon and home-screen banner** — app icon plus the `android:banner` shown on the Android TV leanback home screen.

## Requirements

- Android SDK (configured in this repo via `local.properties` → `sdk.dir`).
- A JDK between 17 and 22. `gradle.properties` currently **pins** `org.gradle.java.home` to a JDK 22 path specific to this machine (newer Android Gradle Plugin + very new JDK versions can clash during the `jlink`/`core-for-system-modules` step). On another machine, update that line to your own JDK path or remove it.
- The Android TV box on the same Wi-Fi network (ADB connects over the network here, no cable needed).

## Build

```bash
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## Finding the TV's IP address

ADB needs the TV's local IP address to connect over the network. Two ways to get it:

**From the TV itself (most reliable):**
Settings → Network & Internet (or "About" / "Device Preferences", naming varies by device) → open your Wi-Fi connection's details → read the "IP address" field.

**From your computer, by scanning the network:**
```bash
# macOS/Linux — lists devices on your local network (adjust the subnet to match your router's)
arp -a | grep -i <tv-brand-or-mac-prefix>
```
or check your router's admin page (often `192.168.1.1`) for the list of connected devices and identify the TV by name.

## Enabling ADB debugging on the TV

1. On the TV: Settings → Device Preferences (or "About") → tap **Build Number** 7 times in a row → this unlocks "Developer Options".
2. Settings → Developer Options → enable **USB Debugging** and **Network ADB Debugging** (exact names vary by device).

## Connecting with ADB and installing

```bash
# Connect using the TV's IP address (default ADB port is 5555)
adb connect 192.168.1.105:5555

# If the TV shows an "Allow USB debugging?" prompt, accept it

# Verify the connection
adb devices
# you should see a line like: 192.168.1.105:5555   device

# Build and install
./gradlew assembleDebug
adb -s 192.168.1.105:5555 install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app (optional — you can also open it manually on the TV)
adb -s 192.168.1.105:5555 shell am start -n com.mehmetsolak.tvgoogle/.MainActivity
```

The `-s 192.168.1.105:5555` flag is only needed when more than one ADB device is connected; you can drop it with just one device attached.

### Notes

- **Icon/banner not updating?** Most launchers cache the icon by package name. `install -r` may not refresh it — run `adb uninstall com.mehmetsolak.tvgoogle` first, then reinstall.
- **Connection shows "offline" or "unauthorized"?** Re-run `adb connect`, or check the TV screen for an authorization prompt you need to accept.
- **IP address changes over time** (DHCP). Setting a static IP for the TV in your router avoids repeating these steps.

## Project layout

```
app/src/main/java/com/mehmetsolak/tvgoogle/
├── MainActivity.java   # WebView plus all cursor/touch/scroll logic
└── CursorView.java     # Draws the cursor icon and computes its hotspot
app/src/main/res/
├── drawable/cursor_icon.png        # Cursor image
├── drawable-nodpi/tv_banner.webp   # Home-screen banner
└── mipmap-*/ic_launcher*.webp      # App icon (per density)
```
