LcdPjt
=======

Chisel translation of  
[Tang-Nano-examples/lcd_pjt](https://github.com/sipeed/Tang-Nano-examples/tree/master/example_lcd/lcd_pjt),  
[TangNano-1K-examples/lcd_pjt](https://github.com/sipeed/TangNano-1K-examples/tree/master/example_lcd/lcd_pjt) and  
[TangNano-9K-example/lcd_led](https://github.com/sipeed/TangNano-9K-example/tree/main/lcd_led)  

## Build this Chisel3 project

### Dependencies

#### JDK 8 or newer

We recommend LTS releases Java 8 and Java 11. You can install the JDK as recommended by your operating system, or use the prebuilt binaries from [AdoptOpenJDK](https://adoptopenjdk.net/).

##### JDK setup on Debian/Ubuntu
```Shell
sudo apt-get update
sudo apt install openjdk-11-jdk
```

#### SBT or mill

SBT is the most common built tool in the Scala community. You can download it [here](https://www.scala-sbt.org/download.html).  
mill is another Scala/Java build tool without obscure DSL like SBT. You can download it [here](https://github.com/com-lihaoyi/mill/releases)

##### SBT setup on Debian/Ubuntu

```Shell
sudo apt-get update
sudo apt-get install apt-transport-https curl gnupg -yqq
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo -H gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
sudo chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
sudo apt-get update
sudo apt-get install sbt
```

#### openFPGALoader

[openFPGALoader](https://github.com/trabucayre/openFPGALoader) Universal utility for programming FPGAs.  
You can use prebuilt binaries from [OSS CAD Suite](https://github.com/YosysHQ/oss-cad-suite-build):

```Shell
wget -N "https://github.com/YosysHQ/oss-cad-suite-build/releases/download/2022-02-05/oss-cad-suite-linux-x64-20220205.tgz"
tar xzf oss-cad-suite-linux-x64-20220205.tgz
source ./oss-cad-suite/environment
```

#### Libraries

To generate the core some additionnal Chisel libraries are required to be
published locally:

- [fpgamacro](https://github.com/Martoni/fpgamacro):

```Shell
git clone https://github.com/Martoni/fpgamacro
cd fpgamacro
sbt publishLocal
cd ..
```

- [HdmiCore](https://github.com/Martoni/HdmiCore):

```Shell
git clone https://github.com/Martoni/HdmiCore
cd HdmiCore
sbt publishLocal
cd ..
```

### How to get started

#### Clone this repository

```sh
git clone https://github.com/scpcom/LcdPjt
cd LcdPjt
```

#### Build the project

supported device type: tangnano tangnano1k tangnano9k  
supported video modes: 480x272 800x480 1024x600  
```sh
sbt -J-Xss256m "runMain hdl.TOPGen tangnano1k 800x480"
```

#### Generate the binary fs

Since the project still uses PLL IP you can not use open source tools to build the binary.  
Open the project in GOWIN FPGA Designer.  
Push the "Run All" button

#### Upload to the device

```sh
openFPGALoader -b tangnano1k impl/pnr/LcdPjt.fs
```

