package hdl

import chisel3._

class VGAMod() extends RawModule {
  val CLK = IO(Input(Clock()))
  val nRST = IO(Input(Bool()))

  val PixelClk = IO(Input(Clock()))

  val LCD_DE = IO(Output(Bool()))
  val LCD_HSYNC = IO(Output(Bool()))
  val LCD_VSYNC = IO(Output(Bool()))

  val LCD_B = IO(Output(UInt(5.W)))
  val LCD_G = IO(Output(UInt(6.W)))
  val LCD_R = IO(Output(UInt(5.W)))

  withClockAndReset(PixelClk, ~nRST) {
  val PixelCount = RegInit("b0".U(16.W))
  val LineCount = RegInit("b0".U(16.W))

  //pluse include in back pluse; t=pluse, sync act; t=bp, data act; t=bp+height, data end
  /*
  val V_BackPorch = 12.U(16.W)
  val V_Pluse = 11.U(16.W)
  val HightPixel = 272.U(16.W)
  val V_FrontPorch = 8.U(16.W)

  val H_BackPorch = 50.U(16.W)
  val H_Pluse = 10.U(16.W)
  val WidthPixel = 480.U(16.W)
  val H_FrontPorch = 8.U(16.W)
  */

  val V_BackPorch = 0.U(16.W) //6
  val V_Pluse = 5.U(16.W)
  val HightPixel = 480.U(16.W)
  val V_FrontPorch = 45.U(16.W) //62

  val H_BackPorch = 182.U(16.W) //NOTE: 高像素时钟时，增加这里的延迟，方便K210加入中断
  val H_Pluse = 1.U(16.W)
  val WidthPixel = 800.U(16.W)
  val H_FrontPorch = 210.U(16.W)

  val PixelForHS = (WidthPixel+H_BackPorch)+H_FrontPorch
  val LineForVS = (HightPixel+V_BackPorch)+V_FrontPorch
  when (PixelCount === PixelForHS) {
    PixelCount := "b0".U(16.W)
    LineCount := LineCount+"b1".U(1.W)
  } .elsewhen (LineCount === LineForVS) {
    LineCount := "b0".U(16.W)
    PixelCount := "b0".U(16.W)
  } .otherwise {
    PixelCount := PixelCount+"b1".U(1.W)
  }

  val Data_R = RegInit("b0".U(10.W))
  val Data_G = RegInit("b0".U(10.W))
  val Data_B = RegInit("b0".U(10.W))

	//注意这里HSYNC和VSYNC负极性
  LCD_HSYNC := (Mux(((PixelCount >= H_Pluse) && (PixelCount <= (PixelForHS-H_FrontPorch))), "b0".U(1.W), "b1".U(1.W)) =/= 0.U)
  //assign  LCD_VSYNC = ((( LineCount  >= 0 )&&( LineCount  <= (V_Pluse-1) )) ) ? 1'b1 : 1'b0;		//这里不减一的话，图片底部会往下拖尾？
  LCD_VSYNC := (Mux((((LineCount >= V_Pluse) && (LineCount <= (LineForVS-0.U)))), "b0".U(1.W), "b1".U(1.W)) =/= 0.U)
  //assign  FIFO_RST  = (( PixelCount ==0)) ? 1'b1 : 1'b0;  //留给主机H_BackPorch的时间进入中断，发送数据
  LCD_DE := (Mux(((((PixelCount >= H_BackPorch) && (PixelCount <= (PixelForHS-H_FrontPorch))) && (LineCount >= V_BackPorch)) && (LineCount <= ((LineForVS-V_FrontPorch)-1.U))), "b1".U(1.W), "b0".U(1.W)) =/= 0.U)
  //这里不减一，会抖动
  LCD_R := Mux((PixelCount < 200.U), "b00000".U(5.W), (Mux(PixelCount < 240.U, "b00001".U(5.W), (Mux(PixelCount < 280.U, "b00010".U(5.W), (Mux(PixelCount < 320.U, "b00100".U(5.W), (Mux(PixelCount < 360.U, "b01000".U(5.W), (Mux(PixelCount < 400.U, "b10000".U(5.W), "b00000".U(5.W))))))))))))
  LCD_G := Mux((PixelCount < 400.U), "b000000".U(6.W), (Mux(PixelCount < 440.U, "b000001".U(6.W), (Mux(PixelCount < 480.U, "b000010".U(6.W), (Mux(PixelCount < 520.U, "b000100".U(6.W), (Mux(PixelCount < 560.U, "b001000".U(6.W), (Mux(PixelCount < 600.U, "b010000".U(6.W), (Mux(PixelCount < 640.U, "b100000".U(6.W), "b000000".U(6.W))))))))))))))
  LCD_B := Mux((PixelCount < 640.U), "b00000".U(5.W), (Mux(PixelCount < 680.U, "b00001".U(5.W), (Mux(PixelCount < 720.U, "b00010".U(5.W), (Mux(PixelCount < 760.U, "b00100".U(5.W), (Mux(PixelCount < 800.U, "b01000".U(5.W), (Mux(PixelCount < 840.U, "b10000".U(5.W), "b00000".U(5.W))))))))))))
  } // withClockAndReset(PixelClk, ~nRST)

}
