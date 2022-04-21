LcdPjt
=======

Chisel translation of [Tang-Nano-examples/lcd_pjt](https://github.com/sipeed/Tang-Nano-examples/tree/master/example_lcd/lcd_pjt)

## Build this Chisel3 project

### Dependencies

#### JDK 8 or newer

We recommend LTS releases Java 8 and Java 11. You can install the JDK as recommended by your operating system, or use the prebuilt binaries from [AdoptOpenJDK](https://adoptopenjdk.net/).

#### SBT or mill

SBT is the most common built tool in the Scala community. You can download it [here](https://www.scala-sbt.org/download.html).  
mill is another Scala/Java build tool without obscure DSL like SBT. You can download it [here](https://github.com/com-lihaoyi/mill/releases)

### How to get started

#### Clone this repository

```sh
git clone git@github.com:scpcom/LcdPjt.git
cd LcdPjt
```

#### Build the project

```sh
sbt "runMain hdl.TOPGen"
```

#### Generate the binary fs

Create new project in GOWIN FPGA Designer, select GW1N-LV1QN48C5/I4 as target device.
Add TOP.v and all files from src/constraints and src/verilog to the project.
Push the "Run All" button

#### Upload to the device

```sh
openFPGALoader -b tangnano impl/pnr/LcdPjt.fs
```

