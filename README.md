EtPushHelloWorld
================
1. Clone and import into Android Studio.
2. Import as non-Android Studio project for AS 1.0+ (previous versions only had 1 option).
3. Replace the strings in readyAimFire() with your IDs.
 _Note: I create a `secrets.xml` file in `res/values` that hold my strings and is automatically excluded from source control by and entry in `.gitignore` included with this source code._

Notes
=====
* App starts with push enabled.
* App starts with location and proximity disabled.
* Proximity button is only enabled if location is enabled *and* bluetooth is available and enabled on the device.
* Any change that results in a RegistrationEvent will kick off a 15 min. timer.
