package hdl
package gowin_pll

import chisel3._
import chisel3.util.Cat
import chisel3.experimental.Param
//Copyright (C)2014-2019 Gowin Semiconductor Corporation.
//All rights reserved.
//File Title: IP file
//GOWIN Version: v1.9.2Beta
//Part Number: GW1N-LV1QN48C6/I5
//Created Time: Fri Oct 25 15:23:07 2019

class Video_PLL() extends RawModule {
    val io = IO(new Bundle{
        val clkout = Output(Clock())
        val lock = Output(Bool())
        val clkoutd = Output(Clock())
        val clkin = Input(Clock())
    })
}

class PLL(val pm: Map[String, Param]) extends BlackBox(pm){
    val io = IO(new Bundle{
        val CLKOUT = Output(Clock())
        val LOCK = Output(UInt(1.W))
        val CLKOUTP = Output(Clock())
        val CLKOUTD = Output(Clock())
        val CLKOUTD3 = Output(Clock())
        val RESET = Input(UInt(1.W))
        val RESET_P = Input(UInt(1.W))
        val RESET_I = Input(UInt(1.W))
        val RESET_S = Input(UInt(1.W))
        val CLKIN = Input(Clock())
        val CLKFB = Input(Bool())
        val FBDSEL = Input(UInt(6.W))
        val IDSEL = Input(UInt(6.W))
        val ODSEL = Input(UInt(6.W))
        val PSDA = Input(UInt(4.W))
        val DUTYDA = Input(UInt(4.W))
        val FDLY = Input(UInt(4.W))
    })
}

//Gowin_PLL^M
class Gowin_PLL() extends Video_PLL {
  val pm: Map[String, Param] = Map(
  "FCLKIN" -> "24",
  "DYN_IDIV_SEL" -> "false",
  "IDIV_SEL" -> 2,
  "DYN_FBDIV_SEL" -> "false",
  "FBDIV_SEL" -> 24,
  "DYN_ODIV_SEL" -> "false",
  "ODIV_SEL" -> 4,
  "PSDA_SEL" -> "0000",
  "DYN_DA_EN" -> "true",
  "DUTYDA_SEL" -> "1000",
  "CLKOUT_FT_DIR" -> "1'b1",
  "CLKOUTP_FT_DIR" -> "1'b1",
  "CLKOUT_DLY_STEP" -> 0,
  "CLKOUTP_DLY_STEP" -> 0,
  "CLKFB_SEL" -> "internal",
  "CLKOUT_BYPASS" -> "false",
  "CLKOUTP_BYPASS" -> "false",
  "CLKOUTD_BYPASS" -> "false",
  "DYN_SDIV_SEL" -> 6,
  "CLKOUTD_SRC" -> "CLKOUT",
  "CLKOUTD3_SRC" -> "CLKOUT",
  "DEVICE" -> "GW1N-1")

  val clkoutp_o = Wire(Clock())
  val clkoutd3_o = Wire(Clock())
  val gw_gnd = Wire(Bool()) 
  gw_gnd := false.B

  val pll_inst = Module(new PLL(pm))

  io.clkout := pll_inst.io.CLKOUT
  io.lock := pll_inst.io.LOCK
  clkoutp_o := pll_inst.io.CLKOUTP
  io.clkoutd := pll_inst.io.CLKOUTD
  clkoutd3_o := pll_inst.io.CLKOUTD3

  pll_inst.io.RESET := gw_gnd
  pll_inst.io.RESET_P := gw_gnd
  pll_inst.io.RESET_I := gw_gnd
  pll_inst.io.RESET_S := gw_gnd
  pll_inst.io.CLKIN := io.clkin
  pll_inst.io.CLKFB := gw_gnd
  pll_inst.io.FBDSEL := Cat(gw_gnd,gw_gnd,gw_gnd,gw_gnd,gw_gnd,gw_gnd)
  pll_inst.io.IDSEL := Cat(gw_gnd,gw_gnd,gw_gnd,gw_gnd,gw_gnd,gw_gnd)
  pll_inst.io.ODSEL := Cat(gw_gnd,gw_gnd,gw_gnd,gw_gnd,gw_gnd,gw_gnd)
  pll_inst.io.PSDA := Cat(gw_gnd,gw_gnd,gw_gnd,gw_gnd)
  pll_inst.io.DUTYDA := Cat(gw_gnd,gw_gnd,gw_gnd,gw_gnd)
  pll_inst.io.FDLY := Cat(gw_gnd,gw_gnd,gw_gnd,gw_gnd)
}
