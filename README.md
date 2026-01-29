# Karoo Shock Tracker

A Karoo extension to track suspension maintenance for mountain bikes based on descent hours.

## Features

- **Front & rear tracking**: Separate service counters for fork and rear shock
- **Actual descent data**: Uses Karoo's elevation loss data for accurate tracking
- **Dual service intervals**: Track both basic service (lowers) and full service (rebuild)
- **Per-bike thresholds**: Configure intervals for each bike
- **In-ride alerts**: Get notified at ride end when service is due
- **Historical hours**: Input past service dates and accumulated hours
- **Bonus actions**: Quick service recording during rides

## How It Works

### Descent Hours
Suspension wear correlates with descending, not total ride time. This app:
1. Captures actual descent meters from your Karoo during each ride
2. Converts to "descent hours" using a configurable rate (default: 300m/hour)
3. Accumulates hours separately for front fork and rear shock

### Service Intervals
Typical manufacturer recommendations:
- **Basic service** (lower leg service, oil change): Every 50-100 descent hours
- **Full service** (complete rebuild): Every 100-200 descent hours

## Installation

### From APK
1. Download the latest APK from [Releases](https://github.com/surfbend/karoo-shock-tracker/releases)
2. Connect your Karoo via USB
3. Install using: `adb install app-debug.apk`

### Build from Source
1. Clone this repository
2. Copy `gradle.properties.template` to `gradle.properties`
3. Add your GitHub credentials (PAT with `read:packages` scope)
4. Build: `./gradlew assembleDebug`
5. Install: `./gradlew installDebug`

## Usage

1. Open the Shock Tracker app on your Karoo
2. Your bikes will automatically sync from Karoo
3. Tap a bike to view fork/shock status and set thresholds
4. Add historical hours if you have existing service records
5. Ride! The app tracks descent automatically
6. Use bonus actions to record services quickly

## Configuration

### Descent Rate
In Settings, configure the descent rate (meters per hour):
- **200-250 m/hr**: Aggressive DH/enduro (more conservative)
- **300 m/hr**: Technical trail riding (default)
- **350-450 m/hr**: Flow trails (less conservative)

### Per-Bike Thresholds
- Basic service interval (hours)
- Full service interval (hours)

## Bonus Actions

During a ride, access these quick actions:
- **Service Front Basic**: Record fork lower service
- **Service Rear Basic**: Record rear shock basic service
- **Check Status**: View current hours for both shocks

## Requirements

- Karoo 2 or Karoo 3
- KOS version 1.541.2070 or later

## License

MIT

## Credits

Built with [karoo-ext](https://github.com/hammerheadnav/karoo-ext) SDK from Hammerhead.
