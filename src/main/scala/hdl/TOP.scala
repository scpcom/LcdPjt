package hdl

import chisel3._
import sv2chisel.helpers.tools.VerilogPortWrapper
//import hdl.gowin_osc.Gowin_OSC
import hdl.gowin_pll.Gowin_PLL

class TOP() extends RawModule {
  val nRST = IO(Input(Bool()))
  val XTAL_IN = IO(Input(Bool()))

  val LCD_CLK = IO(Output(Bool()))
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

  val CLK_SYS = Wire(Bool()) 
  val CLK_PIX = Wire(Bool()) 

  val oscout_o = Wire(Bool()) 
  /*
  //使用内部时钟
  val chip_osc = Module(new Gowin_OSC) //Use internal clock
  oscout_o := chip_osc.oscout //output oscout
  */
  val chip_pll = Module(new Gowin_PLL)
  CLK_SYS := chip_pll.clkout //output clkout      //200M
  CLK_PIX := chip_pll.clkoutd //output clkoutd   //33.33M
  chip_pll.clkin := XTAL_IN //input clkin



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
  val Count = Wire(UInt(32.W)) 
  val rgb_data = Wire(UInt(2.W)) 
  when( !nRST) {
    Count := 0.U(32.W)
    rgb_data := "b00".U(2.W)
  } .elsewhen (Count === 100000000.U) {
    Count := "b0".U(4.W)
    rgb_data := rgb_data+"b1".U(1.W)
  } .otherwise {
    Count := Count+"b1".U(1.W)
  }
  LED_R :=  ~(rgb_data === "b01".U(2.W))
  LED_G :=  ~(rgb_data === "b10".U(2.W))
  LED_B :=  ~(rgb_data === "b11".U(2.W))

}

object TOPGen extends App {
  VerilogPortWrapper.emit(
    () => new TOP(),
    forcePreset = true,
    args = args
  )
}
