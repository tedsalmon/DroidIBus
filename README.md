DroidIBus
=========

Android App that uses a IOIO board to interface with the BMW IBus.

Currently the app is not complete and not all messages that I would like are being decoded. This will soon change.

[Here's a picture of the app in action.](http://i.imgur.com/MUHq5Pq.jpg)

# Goals

* Replace aging BM54 Nav unit in my E46 M3 with a Nexus 7 (2013).
* Retain all features of existing nav unit
* Interface with the car without modifying the wiring harness.


Disclaimer: I'm a pythonista and this is my first Android app and the first time I use Java since AP Computer Science in high school so please forgive me if I'm not up to Java snuff. Feel free to submit pull requests for any changes you feel necessary.

# Features

* Ability to decode any IBus message (Provided you know what it does)
* IKE Data Integration (Fuel Consumption, Speed, Coolant/Outdoor Temperatures)
* Radio Control
* Integrated `Play Music` Interface
* Nav Integration (Nav Data, not Maps)

# Planned Features

* Steering wheel Music control (for Play Music)
* Ability to control the build in amp EQ from within the App
* Built in Map
* G-Sensor Display

# About the Hardware

Right now I can read/write to the IBus through a [IOIO](https://github.com/ytai/ioio) board and an MCP2004 LIN Tranciever but the end goal is to replace the BM54 with the tablet altogether so I will be integrating a Preamp and 3.5mm jack to the board.

[Here's a picture of my breadboard - Schematic coming soon](http://i.imgur.com/GgRS2Hj.jpg)
## Parts List

* IOIO OTG
* MCP2004 LIN Tranciever
* 10k Resistor
* 270 Resistor

## Additional Parts (Not yet Integrated)

* TDA7053A Preamp + Assorted Capacitors for filtering

# Special Thanks

* [BNiles \[kryczech\]](https://github.com/kryczech) for posting the code to his Radio App, Schematics and YouTube videos - without him I wouldn't have gotten this off the ground so quickly.

* [Chris \[Terrapin\]](http://www.startercircuits.com) for posting his original effort online and showing me that this was possible. Also for his cryptic posts across several forums that guided me in my troubleshooting.

