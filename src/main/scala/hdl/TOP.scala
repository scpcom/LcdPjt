package hdl

import chisel3._
import chisel3.util.Cat
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import fpgamacro.gowin.{CLKDIV, Gowin_OSC, Gowin_PLL, Gowin_rPLL, PLLParams, Video_PLL}
import hdmicore.video.{VideoMode,VideoParams,VideoConsts}
import svo.svo_rgb_top

sealed trait DeviceType
case object dtGW1N1 extends DeviceType
case object dtGW1NZ1 extends DeviceType
case object dtGW1NR9 extends DeviceType

class TOP(dt: DeviceType = dtGW1N1, vmode: VideoMode = VideoConsts.m800x480) extends RawModule {
  val nleds = if (dt == dtGW1NR9) 2 else 1
  val nbits = nleds * 3
  val syn_hs_pol = 1
  val syn_vs_pol = 1

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

  val serial_clk = Wire(Clock())
  val pll_lock = Wire(Bool())

  val vout_rst_n = Wire(Bool())

  val pix_clk = Wire(Clock())

  val clk_12M = Wire(Clock())

  /*
  val oscout_o = Wire(Clock())
  //使用内部时钟
  val chip_osc = Module(new Gowin_OSC) //Use internal clock
  oscout_o := chip_osc.io.oscout //output oscout
  */
  def get_pll_par(): PLLParams = {
    if (vmode == VideoConsts.m800x480)
      LCDConsts.m800x480.pll
    else
      vmode.pll
  }
  def get_pll(): Video_PLL = {
    if (dt == dtGW1N1)
      Module(new Gowin_PLL)
    else
      Module(new Gowin_rPLL(get_pll_par()))
  }
  val chip_pll = get_pll
  chip_pll.io.clkin := XTAL_IN //input clkin
  serial_clk := chip_pll.io.clkout //output clkout      //200M
  clk_12M := chip_pll.io.clkoutd //output clkoutd
  pll_lock := chip_pll.io.lock //output lock
  vout_rst_n := nRST & pll_lock

  val uClkDiv = Module(new CLKDIV)
  uClkDiv.io.RESETN := vout_rst_n
  uClkDiv.io.HCLKIN := serial_clk //clk  x5
  pix_clk := uClkDiv.io.CLKOUT //clk  x1   //33.33M
  uClkDiv.io.CALIB := "b1".U(1.W)


  withClockAndReset(serial_clk, ~nRST){
  val cnt_vs = RegInit(0.U(10.W))
  val vs_r = Reg(Bool())
  val vp = vmode.params

  val D1 = Module(new VGAMod(vp))
  D1.io.I_clk := serial_clk
  D1.io.I_rst_n := nRST

  D1.io.I_pxl_clk := pix_clk
  D1.io.I_rd_hres := vp.H_DISPLAY.U
  D1.io.I_rd_vres := vp.V_DISPLAY.U

  val testpattern_inst = Module(new testpattern(vp))
  testpattern_inst.io.I_rst_n := nRST

  testpattern_inst.io.I_pxl_clk := pix_clk
  testpattern_inst.io.I_mode := 0.U(1.W) ## cnt_vs(7,6) //data select //0.U(3.W)
  testpattern_inst.io.I_single_r := 0.U(8.W)
  testpattern_inst.io.I_single_g := 255.U(8.W)
  testpattern_inst.io.I_single_b := 0.U(8.W)
  testpattern_inst.io.I_rd_hres := vp.H_DISPLAY.U
  testpattern_inst.io.I_rd_vres := vp.V_DISPLAY.U
  testpattern_inst.io.I_hs_pol := syn_hs_pol.U(1.W)    //HS polarity , 0:negetive ploarity，1：positive polarity
  testpattern_inst.io.I_vs_pol := syn_vs_pol.U(1.W)    //VS polarity , 0:negetive ploarity，1：positive polarity

  //========================================================================
  val u_svo = Module(new svo_rgb_top(vp))
  u_svo.io.clk := pix_clk
  u_svo.io.resetn := nRST
  u_svo.io.clk_pixel := pix_clk
  u_svo.io.locked := pll_lock
  u_svo.io.term_in_tvalid := false.B //svo_term_valid
  u_svo.io.term_in_tdata := 0.U(8.W) //uart_wdata(7,0)

  //============================================================================

  LCD_DEN  := Mux((cnt_vs <= "h1ff".U(10.W)), testpattern_inst.io.videoSig.de,     Mux((cnt_vs <= "h2ff".U(10.W)), D1.io.videoSig.de,    u_svo.io.videoSig.de))
  LCD_HYNC := Mux((cnt_vs <= "h1ff".U(10.W)), testpattern_inst.io.videoSig.hsync,  Mux((cnt_vs <= "h2ff".U(10.W)), D1.io.videoSig.hsync, u_svo.io.videoSig.hsync))
  LCD_SYNC := Mux((cnt_vs <= "h1ff".U(10.W)), testpattern_inst.io.videoSig.vsync,  Mux((cnt_vs <= "h2ff".U(10.W)), D1.io.videoSig.vsync, u_svo.io.videoSig.vsync))

  LCD_B    := Mux((cnt_vs <= "h1ff".U(10.W)), testpattern_inst.io.videoSig.pixel.blue(7,3),   Mux((cnt_vs <= "h2ff".U(10.W)), D1.io.videoSig.pixel.blue(7,3),  u_svo.io.videoSig.pixel.blue(7,3)))
  LCD_G    := Mux((cnt_vs <= "h1ff".U(10.W)), testpattern_inst.io.videoSig.pixel.green(7,2),  Mux((cnt_vs <= "h2ff".U(10.W)), D1.io.videoSig.pixel.green(7,2), u_svo.io.videoSig.pixel.green(7,2)))
  LCD_R    := Mux((cnt_vs <= "h1ff".U(10.W)), testpattern_inst.io.videoSig.pixel.red(7,3),    Mux((cnt_vs <= "h2ff".U(10.W)), D1.io.videoSig.pixel.red(7,3),   u_svo.io.videoSig.pixel.red(7,3)))

  LCD_CLK := pix_clk

  val vs_in = testpattern_inst.io.videoSig.vsync
  vs_r := vs_in
  when (cnt_vs === "h3ff".U(10.W)) {
    cnt_vs := 0.U
  } .elsewhen (vs_r && ( !vs_in)) { //vs24 falling edge
    cnt_vs := cnt_vs+"b1".U(1.W)
  } .otherwise {
    cnt_vs := cnt_vs
  }

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
  } // withClockAndReset(serial_clk, ~nRST)

}

object TOPGen extends App {
  var devtype: DeviceType = dtGW1N1
  var rd_width = 800
  var rd_height = 480
  var rd_halign = 0
  var rd_valign = 0
  var fullscreen = 1
  var outmode = true
  var vmode: VideoMode = VideoConsts.m800x480

  def set_video_mode(w: Integer, h: Integer, m: VideoMode)
  {
    if (outmode)
      vmode = m
    else {
      rd_width = w
      rd_height = h
    }
  }

  for(arg <- args){
    if ((arg == "GW1N-1") || (arg == "tangnano"))
      devtype = dtGW1N1
    else if ((arg == "GW1NZ-1") || (arg == "tangnano1k"))
      devtype = dtGW1NZ1
    else if ((arg == "GW1NR-9") || (arg == "tangnano9k"))
      devtype = dtGW1NR9

    else if((arg == "wqvga") || (arg == "480x272")){
      set_video_mode(480, 272, LCDConsts.m480x272)
    }
    else if((arg == "vga-15:9") || (arg == "800x480")){
      set_video_mode(800, 480, VideoConsts.m800x480)
    }
    else if((arg == "svga") || (arg == "800x600")){
      set_video_mode(800, 600, VideoConsts.m800x600)
    }
    else if((arg == "480p") || (arg == "720x480")){
      set_video_mode(720, 480, VideoConsts.m720x480)
    }
    else if((arg == "sd") || (arg == "576p") || (arg == "720x576")){
      set_video_mode(720, 576, VideoConsts.m720x576)
    }
    else if((arg == "wsvga") || (arg == "1024x600")){
      set_video_mode(1024, 600, VideoConsts.m1024x600)
    }
    else if((arg == "xga") || (arg == "1024x768")){
      set_video_mode(1024, 768, VideoConsts.m1024x768)
    }
    else if((arg == "hd") || (arg == "720p") || (arg == "1280x720")){
      set_video_mode(1280, 720, VideoConsts.m1280x720)
    }
    else if((arg == "wxga") || (arg == "1280x800")){
      set_video_mode(1280, 800, VideoConsts.m1280x800)
    }
    else if((arg == "sxga") || (arg == "1280x1024")){
      set_video_mode(1280, 1024, VideoConsts.m1280x1024)
    }
    else if(arg == "1360x768"){
      set_video_mode(1360, 768, VideoConsts.m1360x768)
    }
    else if(arg == "1366x768"){
      set_video_mode(1366, 768, VideoConsts.m1366x768)
    }
    else if(arg == "1440x900"){
      set_video_mode(1440, 900, VideoConsts.m1440x900)
    }
    else if((arg == "wsxga") || (arg == "1600x900")){
      set_video_mode(1600, 900, VideoConsts.m1600x900)
    }
  }
  if (devtype == dtGW1N1)
    println("Building for tangnano")
  else if (devtype == dtGW1NZ1)
    println("Building for tangnano1k")
  else if (devtype == dtGW1NR9)
    println("Building for tangnano9k")

  (new ChiselStage).execute(args,
    Seq(ChiselGeneratorAnnotation(() => new TOP(devtype, vmode))))
}
