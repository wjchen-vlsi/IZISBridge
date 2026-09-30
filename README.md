# IZISBridge

> **⚠️ DISCLAIMER**
> This repository is the result of personal reverse engineering research. The author makes no guarantees regarding its accuracy, safety, or whether it aligns with Izis's original design intentions. Incorrect usage of these commands may potentially cause damage to the hardware. The author is in no way affiliated with Izis. Furthermore, there is no guarantee that Izis will not update their system in the future to block or restrict third-party development. Use this information entirely at your own risk.

## Overview
IZISBridge is an Android application meant to be run on the  **IZIS (隱智) Smart Go Board**. The purpose of is to forward the IZIS Smart Go Board serial device over TCP for easier third-party application development.
For details on preparing the devlopment environment, see [IZIS Smart Go Board - Third-Party Development Kit & Reverse Engineering Specs](https://github.com/wjchen-vlsi/izis_go_board_reng)

```text
[ PC Local Environment ]                                     [ Physical Izis Smart Go Board ]
                                       Wi-Fi (TCP)
+-----------------------+                                    +----------------------------------+
|   Android Emulator    |                                    | Android OS (IZISBridge)          |
|   or Local PC App     |          192.168.X.X:5000          |                                  |
|                       | <--------------------------------> | Listens on Port: 5000            |
| Connects to:          |                                    | Forwards to: /dev/ttyS1          |
| <BOARD_IP>:5000       |                                    |                                  |
+-----------------------+                                    +---------|------------------------+
                                                                       | UART (/dev/ttyS1)
                                                                       v
                                                             +----------------------------------+
                                                             | Go Board Hardware MCU            |
                                                             | (LEDs, Sensors, Buttons)         |
                                                             +----------------------------------+
```
