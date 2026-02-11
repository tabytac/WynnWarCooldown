Wynn War Cooldown
=================

Wynn War Cooldown is a lightweight Wynncraft mod that tracks war cooldowns and shows them on-screen with optional sound alerts and auto-attack.

This mod depends on Wynntils, so make sure it is installed as it is needed to detect territory changes and other information.

Features
========
- Detects war cooldown messages and starts timers automatically.
- Detects when other guilds attack your territory and automatically starts timers for those.
- Displays active timers on a configurable HUD.
- Plays a configurable sound when timers end.
- Optional auto-command: /guild attack when cooldown ends.
- Configurable settings for HUD placement, sound options, and more.

Configuration
=============
Use Mod Menu to open the configuration screen and customize HUD placement, sound options, and other settings.

How to Compile
==============
1. Clone the repo and open it in IntelliJ or VS Code as a Gradle project.
2. Run the Gradle build task:
   - Windows: .\gradlew.bat build
   - Mac/Linux: ./gradlew build
3. The built jar will be in build/libs.

License
=======
See LICENSE.
