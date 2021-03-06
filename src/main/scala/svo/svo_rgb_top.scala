package svo

import chisel3._
import chisel3.util.Cat
import fpgamacro.gowin.{OSER10,Oser10Module,ELVDS_OBUF,TLVDS_OBUF}
import hdmicore.VideoHdmi
import hdmicore.video.{VideoParams, VideoConsts}
/*
 *  SVO - Simple Video Out FPGA Core
 *
 *  Copyright (C) 2014  Clifford Wolf <clifford@clifford.at>
 *  
 *  Permission to use, copy, modify, and/or distribute this software for any
 *  purpose with or without fee is hereby granted, provided that the above
 *  copyright notice and this permission notice appear in all copies.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

// `timescale1ns/1ps

case class SvoParams(
    val SVO_FRAMERATE: Int,
    val SVO_BITS_PER_PIXEL: Int,
    val SVO_BITS_PER_RED: Int,
    val SVO_BITS_PER_GREEN: Int,
    val SVO_BITS_PER_BLUE: Int,
    val SVO_BITS_PER_ALPHA: Int,
    val SVO_XYBITS: Int
)

package object SvoConsts {
  val DefaultParams = SvoParams(
    SVO_FRAMERATE = 60,
    SVO_BITS_PER_PIXEL = 24,
    SVO_BITS_PER_RED = 8,
    SVO_BITS_PER_GREEN = 8,
    SVO_BITS_PER_BLUE = 8,
    SVO_BITS_PER_ALPHA = 0,
    SVO_XYBITS = 14
  )
}

class svo_rgb_top(vp: VideoParams = VideoConsts.m800x600.params) extends RawModule {
val io = IO(new Bundle {
  val clk = Input(Clock())
  val resetn = Input(Bool())

  // video clocks
  val clk_pixel = Input(Clock())
  val locked = Input(Bool())

  val term_in_tvalid = Input(Bool())
  val term_out_tready = Output(Bool())
  val term_in_tdata = Input(UInt(8.W))

  // output signals
  val video_resetn = Output(Bool())
  val videoSig = Output(new VideoHdmi())
})

  val sp: SvoParams = SvoConsts.DefaultParams

  val white_pixval = ~(0.U(sp.SVO_BITS_PER_PIXEL.W))

  val vdma_tvalid = Wire(Bool()) 
  val vdma_tready = Wire(Bool()) 
  val vdma_tdata = Wire(UInt(sp.SVO_BITS_PER_PIXEL.W))
  val vdma_tuser = Wire(UInt(1.W)) 

  val video_tvalid = Wire(Bool()) 
  val video_tready = Wire(Bool()) 
  val video_tdata = Wire(UInt(sp.SVO_BITS_PER_PIXEL.W))
  val video_tuser = Wire(UInt(1.W)) 

  val term_in_tready = Wire(Bool())
  val term_out_tvalid = Wire(Bool()) 
  //val term_out_tready = Wire(Bool())
  val term_out_tdata = Wire(UInt(2.W)) 
  val term_out_tuser = Wire(UInt(1.W)) 
  val video_enc_tvalid = Wire(Bool()) 
  val video_enc_tready = Wire(Bool()) 
  val video_enc_tdata = Wire(UInt(sp.SVO_BITS_PER_PIXEL.W))
  val video_enc_tuser = Wire(UInt(4.W))

  withClockAndReset(io.clk, ~io.resetn) {
  val locked_clk_q = Reg(UInt(4.W)) 
  val resetn_clk_pixel_q = Reg(UInt(4.W)) 
  locked_clk_q := Cat(locked_clk_q, io.locked)
  resetn_clk_pixel_q := Cat(resetn_clk_pixel_q, io.resetn)

  val clk_resetn = WireDefault(Bool(), io.resetn && locked_clk_q(3))
  val clk_pixel_resetn = WireDefault(Bool(), io.locked && resetn_clk_pixel_q(3))

  val svo_tcard = Module(new svo_tcard(
    vp = vp,
    sp = sp
  ))
  svo_tcard.io.clk := io.clk_pixel
  svo_tcard.io.resetn := io.resetn

  vdma_tvalid := svo_tcard.io.out_axis_tvalid
  svo_tcard.io.out_axis_tready := vdma_tready
  vdma_tdata := svo_tcard.io.out_axis_tdata
  vdma_tuser := svo_tcard.io.out_axis_tuser

  val svo_term = Module(new svo_term(
    vp = vp,
    sp = sp
  ))
  svo_term.io.clk := io.clk
  svo_term.io.oclk := io.clk_pixel
  svo_term.io.resetn := clk_resetn

  svo_term.io.in_axis_tvalid := io.term_in_tvalid
  term_in_tready := svo_term.io.in_axis_tready
  svo_term.io.in_axis_tdata := io.term_in_tdata

  term_out_tvalid := svo_term.io.out_axis_tvalid
  svo_term.io.out_axis_tready := io.term_out_tready
  term_out_tdata := svo_term.io.out_axis_tdata
  term_out_tuser := svo_term.io.out_axis_tuser

  val over_axis_tuser = Wire(UInt(2.W))
  over_axis_tuser := (term_out_tdata === "b10".U(2.W)) ## term_out_tuser

  val svo_overlay = Module(new svo_overlay(
    vp = vp,
    sp = sp
  ))
  svo_overlay.io.clk := io.clk_pixel
  svo_overlay.io.resetn := clk_pixel_resetn
  svo_overlay.io.enable := "b1".U(1.W)

  svo_overlay.io.in_axis_tvalid := vdma_tvalid
  vdma_tready := svo_overlay.io.in_axis_tready
  svo_overlay.io.in_axis_tdata := vdma_tdata
  svo_overlay.io.in_axis_tuser := vdma_tuser

  svo_overlay.io.over_axis_tvalid := term_out_tvalid
  io.term_out_tready := svo_overlay.io.over_axis_tready
  svo_overlay.io.over_axis_tdata := white_pixval
  svo_overlay.io.over_axis_tuser := over_axis_tuser

  video_tvalid := svo_overlay.io.out_axis_tvalid
  svo_overlay.io.out_axis_tready := video_tready
  video_tdata := svo_overlay.io.out_axis_tdata
  video_tuser := svo_overlay.io.out_axis_tuser

  val svo_enc = Module(new svo_enc(
    vp = vp,
    sp = sp
  ))
  svo_enc.io.clk := io.clk_pixel
  svo_enc.io.resetn := clk_pixel_resetn

  svo_enc.io.in_axis_tvalid := video_tvalid
  video_tready := svo_enc.io.in_axis_tready
  svo_enc.io.in_axis_tdata := video_tdata
  svo_enc.io.in_axis_tuser := video_tuser

  video_enc_tvalid := svo_enc.io.out_axis_tvalid
  svo_enc.io.out_axis_tready := video_enc_tready
  video_enc_tdata := svo_enc.io.out_axis_tdata
  video_enc_tuser := svo_enc.io.out_axis_tuser
  video_enc_tready := true.B

  io.video_resetn := clk_pixel_resetn
  io.videoSig.de := !video_enc_tuser(3)
  io.videoSig.vsync := video_enc_tuser(2)
  io.videoSig.hsync := video_enc_tuser(1)
  io.videoSig.pixel.blue := video_enc_tdata(23,16)
  io.videoSig.pixel.green := video_enc_tdata(15,8)
  io.videoSig.pixel.red := video_enc_tdata(7,0)
  }
}
