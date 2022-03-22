package hdl

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
//import hdl.gowin_osc.Gowin_OSC
import hdl.gowin_pll.Gowin_PLL

class TOP() extends RawModule {
  val nRST = IO(Input(Bool()))
  val XTAL_IN = IO(Input(Clock()))

  val LCD_CLK = IO(Output(Clock()))
  val LCD_HYNC = IO(Output(Bool()))
  val LCD_SYNC = IO(Output(Bool()))
  val LCD_DEN = IO(Output(Bool()))
  val LCD_R = IO(Output(UInt(5.W)))
  val LCD_G = IO(Output(UInt(6.W)))
  val LCD_B = IO(Output(UInt(5.W)))

  val LED_R = IO(Output(Bool()))
  val LED_G = IO(Output(Bool()))
  val LED_B = IO(Output(Bool()))
  val KEY = IO(Input(Bool()))

  val CLK_SYS = Wire(Clock())
  val CLK_PIX = Wire(Clock())

  /*
  val oscout_o = Wire(Clock())
  //使用内部时钟
  val chip_osc = Module(new Gowin_OSC) //Use internal clock
  oscout_o := chip_osc.io.oscout //output oscout
  */
  val chip_pll = Module(new Gowin_PLL)
  CLK_SYS := chip_pll.io.clkout //output clkout      //200M
  CLK_PIX := chip_pll.io.clkoutd //output clkoutd   //33.33M
  chip_pll.io.clkin := XTAL_IN //input clkin



  withClockAndReset(CLK_SYS, ~nRST){
  val D1 = Module(new VGAMod)
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
  val Count = RegInit(0.U(32.W)) 
  val rgb_data = RegInit("b00".U(2.W)) 
  when (Count === 100000000.U) {
    Count := "b0".U(4.W)
    rgb_data := rgb_data+"b1".U(1.W)
  } .otherwise {
    Count := Count+"b1".U(1.W)
  }
  LED_R :=  ~(rgb_data === "b01".U(2.W))
  LED_G :=  ~(rgb_data === "b10".U(2.W))
  LED_B :=  ~(rgb_data === "b11".U(2.W))
  } // withClockAndReset(CLK_SYS, ~nRST)

}

object TOPGen extends App {
  (new ChiselStage).execute(args,
    Seq(ChiselGeneratorAnnotation(() => new TOP())))
}
