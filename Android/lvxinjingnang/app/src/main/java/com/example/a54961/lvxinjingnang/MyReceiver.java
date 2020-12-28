package com.example.a54961.lvxinjingnang;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver { // 一个继承了广播接收器的抽象类，用于将接收到的内容通过接口传递出去
    private Message message;

    @Override
    public void onReceive(Context context, Intent intent) { // 重写了一个系统广播接收的方法，用于处理接收到的广播内容intent
        message.getMsg(intent.getStringExtra("city") + "\n" + intent.getStringExtra("lo") + "\n" + intent.getStringExtra("la"));
        // 将接收到的广播intent里的内容提取出来组成字符串str发给接口函数
    }

    interface Message { // 定义了一个接口，内包括一个getMsg方法，用于处理接收到的包含了位置数据的字符串 str
        public void getMsg(String str);
    }

    public void setMessage(Message message){ // 绑定
        this.message = message;
    }
}
