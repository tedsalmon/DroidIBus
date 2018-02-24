DroidIBus
=========

Android App that uses an MCP2004 and a IOIO board to interface with the BMW IBus

[Here's a picture of the app in action.](http://i.imgur.com/4ZdpFgc.png)

# Project Goals

* Replace aging BMBT Nav unit in my E46 M3 with a Nexus 7 (2013).
* Retain all features of existing nav unit
* Interface with the car without modifying the wiring harness.

# Features

* Ability to decode any IBus message (Provided you know what it does)
* IKE Metrics Integration (Fuel Consumption, Speed, Coolant/Outdoor Temperatures)
* BM53 (Remote Radio) Control
* Integrated Android Music Player
* Nav Integration (Nav Data, not Maps)
* Steering wheel next/previous media control
* Ability to set car settings such as date, time, units of measure, etc
* Support for the ValentineOne "StealthOne" - an IBus specific V1 add-on

# Future features

* Add option to unlock doors on key removal - Useful for people who have auto-lock coded in
* Add some kind of "Mini App" that stays on top of all windows - Useful when navigating
* TCU Interfacing as much as possible

# Requirements

* Android Device with IOIO support
* Android 5.0+ - Required due to the Music Player interface 
* IOIO Board w/ IOIO App-IOIO0503

# About the Hardware

UPDATE: I'm currently working on a PCB with the IOIO integrated. The PCB will feature:

* PIC24 MCU
* 5v Switching Regulator
* 3.3v Linear Regulator
* TDA7053A PreAmp
* MCP2004
* USB DCP With a TPS2540 for 1.5A charging while in use

Right now I can read/write to the IBus through a [IOIO](https://github.com/ytai/ioio) board and an MCP2004 LIN Tranciever but the end goal is to replace the BM53 with the tablet altogether so I will be integrating a Preamp and 3.5mm jack to the board.

[Here's a picture of my breadboard](http://i.imgur.com/GgRS2Hj.jpg)

[Here is the basic Fritzing design of the board](https://docs.google.com/file/d/0B_R-TsYhwbCcc2xtSU5VSWpKTUU)

## Parts List

* IOIO OTG
* MCP2004 LIN Tranciever
* 2x 10k 1/4 watt resistor (Note: These values assume a supply voltage of 3.3V)

# Special Thanks

* [BNiles \[kryczech\]](https://github.com/kryczech) for posting the code to his Radio App, Schematics and YouTube videos - without him I wouldn't have gotten this off the ground so quickly.

* [Chris \[Terrapin\]](http://www.startercircuits.com) for posting his original effort online and showing me that this was possible. Also, for finally posting the schematics for his PCB which made the job of integrating the TDA7053 easy.
