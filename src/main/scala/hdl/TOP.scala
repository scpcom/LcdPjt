package hdl

import chisel3._
import chisel3.util.Cat
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import fpgamacro.gowin.{Gowin_OSC, Gowin_PLL, Gowin_rPLL, PLLParams, Video_PLL}
import hdmicore.video.{VideoParams}

sealed trait DeviceType
case object dtGW1N1 extends DeviceType
case object dtGW1NZ1 extends DeviceType
case object dtGW1NR9 extends DeviceType

class TOP(dt: DeviceType = dtGW1N1) extends RawModule {
  val nleds = if (dt == dtGW1NR9) 2 else 1
  val nbits = nleds * 3

  val nRST = IO(Input(Bool()))
  val XTAL_IN = IO(Input(Clock()))

  val LCD_CLK = IO(Output(Clock()))
  val LCD_HYNC = IO(Output(Bool()))
  val LCD_SYNC = IO(Output(Bool()))
  val LCD_DEN = IO(Output(Bool()))
  val LCD_R = IO(Output(UInt(5.W)))
  val LCD_G = IO(Output(UInt(6.W)))
  val LCD_B = IO(Output(UInt(5.W)))

  val LED = IO(Output(UInt(nbits.W)))
  val User_Button = IO(Input(Bool()))

  val CLK_SYS = Wire(Clock())
  val CLK_PIX = Wire(Clock())

  //pluse include in back pluse; t=pluse, sync act; t=bp, data act; t=bp+height, data end
  /*
  val vp = VideoParams(
        V_BOTTOM = 12,
        V_BACK = 12,
        V_SYNC = 11,
        V_DISPLAY = 272,
        V_TOP = 8,

        H_BACK = 50,
        H_SYNC = 10,
        H_DISPLAY = 480,
        H_FRONT = 8,
    )
  */

  val vp = VideoParams(
        V_BOTTOM = 0,
        V_BACK = 0, //6
        V_SYNC = 5,
        V_DISPLAY = 480,
        V_TOP = 45, //62

        H_BACK = 182, //NOTE: 高像素时钟时，增加这里的延迟，方便K210加入中断
        H_SYNC = 1,
        H_DISPLAY = 800,
        H_FRONT = 210,
    )

  /*
  val oscout_o = Wire(Clock())
  //使用内部时钟
  val chip_osc = Module(new Gowin_OSC) //Use internal clock
  oscout_o := chip_osc.io.oscout //output oscout
  */
  def get_pll(): Video_PLL = {
    /* 27 MHz * (36+1) / (4+1) = 199.8 MHz / 6 = 33.3 MHz ; 199.8 MHz / 5 = 39.96 MHz */
    val p39960khz  = PLLParams(IDIV_SEL = 4, FBDIV_SEL = 36, ODIV_SEL = 4, DYN_SDIV_SEL = 6)
    if (dt == dtGW1N1)
      Module(new Gowin_PLL)
    else
      Module(new Gowin_rPLL(p39960khz))
  }
  val chip_pll = get_pll
  CLK_SYS := chip_pll.io.clkout //output clkout      //200M
  CLK_PIX := chip_pll.io.clkoutd //output clkoutd   //33.33M
  chip_pll.io.clkin := XTAL_IN //input clkin



  withClockAndReset(CLK_SYS, ~nRST){
  val D1 = Module(new VGAMod(vp))
  D1.CLK := CLK_SYS
  D1.nRST := nRST

  D1.PixelClk := CLK_PIX
  LCD_DEN := D1.LCD_DE
  LCD_HYNC := D1.LCD_HSYNC
  LCD_SYNC := D1.LCD_VSYNC

  LCD_B := D1.LCD_B
  LCD_G := D1.LCD_G
  LCD_R := D1.LCD_R
  LCD_CLK := CLK_PIX

  //RGB LED TEST
  val cnmax = (100000000 / nleds).U // 9k with XTAL_IN: "d400_0000".U(24.W), 1k with XTAL_IN: "d1350_0000".U(31.W)
  val counter = RegInit(0.U(32.W))
  val ledbits = RegInit(("b"+"1"*(nbits-1)+"0").U(nbits.W)) // 9k "b111110".U(6.W), 1k "b110".U(3.W)
  when (counter < cnmax) { // 0.5s delay
    counter := counter+"b1".U(1.W)
  } .otherwise {
    counter := "b0".U(31.W)
    ledbits := Cat(ledbits(nbits-2,0), ledbits(nbits-1))
  }
  LED := ledbits
  } // withClockAndReset(CLK_SYS, ~nRST)

}

object TOPGen extends App {
  var devtype: DeviceType = dtGW1N1

  for(arg <- args){
    if ((arg == "GW1N-1") || (arg == "tangnano"))
      devtype = dtGW1N1
    else if ((arg == "GW1NZ-1") || (arg == "tangnano1k"))
      devtype = dtGW1NZ1
    else if ((arg == "GW1NR-9") || (arg == "tangnano9k"))
      devtype = dtGW1NR9
  }
  if (devtype == dtGW1N1)
    println("Building for tangnano")
  else if (devtype == dtGW1NZ1)
    println("Building for tangnano1k")
  else if (devtype == dtGW1NR9)
    println("Building for tangnano9k")

  (new ChiselStage).execute(args,
    Seq(ChiselGeneratorAnnotation(() => new TOP(devtype))))
}
