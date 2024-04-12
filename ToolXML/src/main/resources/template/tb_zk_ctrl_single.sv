`timescale 1ns/1ps

module {testCaseName};

tb tb();

typedef struct{
    {structValNameList}
	}zk_ctrl_fr;
	
initial begin
  //设置控制指令帧内容
  zk_ctrl_fr fr1 = {{frameContentList}};
  tb.baud_base = 115200; //波特率设置,24bits
  tb.CheckInd = 1; //1：校验和正确
	
  //等待DUT脱离复位
  tb.delay_ms(0.1);  //delay 0.1ms
  //遥控指令
  tb.zkControlTCcmd({{cmdSendBody}});
 
  tb.delay_ms(0.1);  //delay 0.1ms

  #100 $stop;

end

endmodule