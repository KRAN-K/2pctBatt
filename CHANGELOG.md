# Changelog

## v1.2
- Added an App Power & Data section with per-app foreground activity and Wi-Fi/mobile data totals for the last 24 hours.
- Added Usage Access onboarding and a shortcut to Android's built-in battery usage screen for actual per-app battery drain where supported.
- Added a resizable App Usage widget showing the most active app and top data users.
- Added compact, medium and expanded widget content modes based on launcher-provided widget dimensions.
- Synchronized dark/light appearance and selected accent color across the app and all widgets.
- Added app usage permissions and updated the app version to 1.2.

Notes:
- Android does not expose reliable per-app battery consumption to ordinary third-party apps. The in-app activity ranking is a foreground-time clue, not measured battery drain; use Android's battery usage screen for the device's actual per-app battery estimates.
- Per-app network totals depend on Android's Usage Access / NetworkStats support and may be unavailable or incomplete on some manufacturer builds.

## v1.1
- Added a separate resizable Network home-screen widget.
- Displays the active network's local IPv4 address and public IP address.
- Added manual download and upload throughput tests using Cloudflare's public speed-test endpoints.
- Stores the latest speed-test results and displays them in the widget.
- Added network permissions and a dedicated Network Status screen.

## v1.0
- Initial dashboard for battery, RAM, internal storage and power information.
- Live monitoring screen with periodic refresh.
- Diagnostics with actionable recommendations.
- Dark and light themes with six accent colors.
- Resizable home-screen widget showing battery, RAM and storage.
- GitHub Actions APK build workflow.
