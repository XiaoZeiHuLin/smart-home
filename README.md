# smart-home
将家居物联网化，通过手机app和各种居家传感器收集用户生活信息，通过一些算法进行归类和预测。同时也可通过网页端和移动端app与家居电器进行交互。

1.	运行环境
1.1.	硬件环境
硬件搭建基于Arduino Mega 2560开发板，手机端采用Android设备运行程序
1.2.	软件环境
通过Arduino IDE来编辑和烧录控制开发板的程序，通过Android Studio来编写Android设备上运行的APP，通过PyCharm Pro上创建的Django项目构建服务端，通过MySQL存储数据信息。
2.	软件安装与配置
2.1.	安装Java
2.2.	安装Python
2.3.	安装Arduino
2.4.	安装Android Studio
2.5.	安装PyCharm Pro
2.6.	安装MySQL
3.	数据导入
3.1.	导入Arduino项目
3.2.	导入Android项目
3.3.	导入Django项目
3.4.	导入MySQL数据库
4.	系统部署与配置
4.1.	配置Arduino
检查串口序列号，在Arduino IDE的设置界面找到正在通过USB连接开发板的串口序列号，并且在开发板芯片组类型中选择Arduino Mega 2560。
4.2.	配置Android
打开Android设备的USB调试模式，并允许电脑安装软件。
4.3.	配置Django
在views.py文件中，将所有在串口读写初始化模块中的”COM”改成本机对应的串口序列号。在Run菜单下找到项目启动配置选项，打开后在manage文件的启动项后添加 “runserver 0.0.0.0:8080”。
5.	系统启动
5.1.	启动Arduino
将开发板接上串口线，另一端连上电脑的USB接口。
5.2.	启动Android
将Android设备通过数据线接上电脑后，确认Android设备的USB调试模式已打开，然后在Android Studio上运行APP，运行目标为该Android设备，安装APP完毕后在设置界面的应用管理界面打开该APP所需要的所有权限。
5.3.	启动Django
右键manage.py文件，点击Run来运行Django服务。
