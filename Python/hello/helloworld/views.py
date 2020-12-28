from statsmodels.tsa.api import ExponentialSmoothing
from django.shortcuts import render, render_to_response
from django.template import RequestContext
from django.http import HttpResponse
from PIL import Image
import matplotlib.pyplot as plt
import mysql.connector
import serial
import time
import threading
import numpy as np
import pandas
import requests
import csv

flag_r = False
flag_g = False
flag_b = False
servo = False


class cluster(threading.Thread):
    def __init__(self, k):
        threading.Thread.__init__(self)
        self.k = k

    def loadOriginalData(self):
        data = []
        sub_data = []

        con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
        cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
        cursor.execute(u"use django;")  # 执行语句：使用名叫“django”的这个数据库

        sql = "select longitude FROM django.route;"
        cursor.execute(sql)  # 执行该sql语句
        result_longitude = cursor.fetchall()
        lo = cursor.rowcount

        sql = "select latitude FROM django.route;"
        cursor.execute(sql)  # 执行该sql语句
        result_latitude = cursor.fetchall()
        la = cursor.rowcount

        con.commit()  # 提交
        cursor.close()

        if la == lo:
            while la > 0:
                sub_data.append(result_longitude[la - 1][0])
                sub_data.append(result_latitude[la - 1][0])
                data.append(sub_data)
                sub_data = []
                la = la - 1

        return data

    def Eclud(self, point, centroid):  # 欧式距离公式
        return np.sqrt(np.sum(np.power(point - centroid, 2)))

    def OriginalCentroids(self, dataSet, categories):  # 在最大最小值范围内等比例随机生成所有中心点的第一维、第二维、
        dimension = np.shape(dataSet)[1]
        Centroids = np.mat(np.zeros([categories, dimension]))
        for i in range(dimension):
            Max = max(dataSet[:, i])
            Min = min(dataSet[:, i])
            Range = float(Max - Min)
            Centroids[:, i] = float(Min) + Range * np.random.rand(categories, 1)  # 在最大与最小范围内等比例随机生成所有中心点的第一维、第二维、
        return Centroids

    def K_Means(self, dataSet, k):
        dataSet_length = np.shape(dataSet)[0]  # dataSet矩阵的长度
        centroids_length = k  # centroids矩阵的长度
        clusterAssignment = np.mat(np.zeros([dataSet_length, 2]))  # 生成并初始化clusterAssment矩阵
        centroids = self.OriginalCentroids(dataSet, k)  # 调用OriginalCentroids()生成包含随机k个中心点的矩阵
        currentcluster = np.mat(np.zeros([dataSet_length, 1]))  # 生成并初始化currentcluster矩阵，用于额外再次存储每次计算出的data点所属信息，用来与下次结果做比较，以此来判断是否计算完成
        clusterChanged = True  # flag

        while clusterChanged:  # 循环逼近
            clusterChanged = False  # flag

            # 把每一个数据点划分到离它最近的中心点
            for dataSet_Cur in range(dataSet_length):
                clusterAssignment[dataSet_Cur, 1] = float('inf')  # 给每一个data点距其中心点的距离默认设置为无穷大
                for centroids_Cur in range(centroids_length):
                    current_dist = self.Eclud(dataSet[dataSet_Cur], centroids[centroids_Cur])  # 循环实现计算出每个data点距离当前所有中心点的距离
                    if current_dist < clusterAssignment[dataSet_Cur, 1]:  # 只要发现该距离小于当前自身距离最近中心点的距离（第一次默认为无穷大），就拿该中心点作为data点的新的中心点，并更新其所属新的中心点的种类和距离
                        clusterAssignment[dataSet_Cur, 1] = current_dist
                        clusterAssignment[dataSet_Cur, 0] = centroids_Cur

            # 重新计算中心点
            for centroids_Cur in range(centroids_length):
                current = []
                for dataSet_Cur in range(dataSet_length):
                    if clusterAssignment[dataSet_Cur, 0] == centroids_Cur:  # 寻找和当前中心点同色的data点
                        current.append(dataSet[dataSet_Cur, :])  # 如果找到，就将其data点的坐标添加到current内
                centroids[centroids_Cur] = np.mean(current, 0)  # 计算出current内的（当前同色的）所有data点的新的中心点坐标并覆盖掉原来的中心点坐标

            for dataSet_Cur in range(dataSet_length):  # 判断是否可以结束循环
                if clusterAssignment[dataSet_Cur, 0] != currentcluster[dataSet_Cur, 0]:  # 将当前每个data点的所属信息和距离信息与上一次比较，如果有变化则表示仍在更新，允许继续循环。同时更新上一次的所属信息和距离信息为本次的信息，为下次做准备
                    clusterChanged = True
                    currentcluster[dataSet_Cur, 0] = clusterAssignment[dataSet_Cur, 0]

        return centroids

    def start(self):
        dataMatrix = np.mat(self.loadOriginalData())
        centroids = self.K_Means(dataMatrix, self.k)
        return centroids


def geocode(location):
    parameters = {'location': location, 'key': 'a7a3f14e749b70063b8b6c228c588fa4'}
    base = 'http://restapi.amap.com/v3/geocode/regeo'
    response = requests.get(base, parameters)
    answer = response.json()
    return answer


def frequent_location_number(request):
    return render(request, 'frequent_location_number.html')


def analyse_cluster(request):
    addresses = ""
    frequent_location_number = int(request.POST.get('frequent_location_number'))
    c = cluster(frequent_location_number)
    centroids = c.start()
    # print(centroids)
    for i in centroids:
        location = str(round(i.tolist()[0][0], 6)) + "," + str(round(i.tolist()[0][1], 6))
        try:
            address = geocode(location)['regeocode']['formatted_address']
            addresses = addresses + address + "<br>"
        except:
            print("经纬度为空，无法获取地址")
    return HttpResponse(addresses)


class Acquire(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self):
        data_last = []
        ser = serial.Serial()  # 初始化串口对象
        ser.baudrate = 9600  # 设定串口波特率
        ser.port = 'COM5'  # 设定串口名
        ser.open()  # 设定完成，打开串口
        ser.readline()  # 放空第1行可能是残缺的数据
        ser.readline()  # 放空第2行可能是残缺的数据
        Day = ""

        while 1:
            all_data = ser.readline().decode("UTF-8")  # 通过串口读取一行数据，由于该数据是二进制格式的，需要转码成 UTF-8 格式才可读
            sensors_data = all_data.split(';')[0]  # 记录前半部分的传感器数据
            locate_data = all_data.split(';')[1]  # 记录后半部分的定位数据
            data_str = locate_data.rstrip('\r\n')  # 将后半部分去掉换行符

            con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
            cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
            cursor.execute(u"use django;")  # 执行语句：使用名叫“django”的这个数据库

            if Day != time.asctime().split(" ")[2]:  # 如果此刻是新的一天的开始
                Day = time.asctime().split(" ")[2]
                DailyScan()                          # 则扫描 Daily 表上的昨天的传感器组合信息，汇聚到 .CSV 表格中，并清空 Daily 数据库

            if sensors_data != data_last:
                entrance = sensors_data.split(" ")[0]  # 将读取的一行数据拆分，取第一个参数，是超声波传感器的检测数据
                seat = sensors_data.split(" ")[1]  # 将读取的一行数据拆分，取第二个数据，是压力传感器的检测数据
                window = sensors_data.split(" ")[2]  # 将读取的一行数据拆分，取第三个数据，是门磁传感器的检测数据
                pipe = sensors_data.split(" ")[3]  # 将读取的一行数据拆分，取第四个数据，是水流霍尔传感器的检测数据
                light = sensors_data.split(" ")[4]
                temperature = sensors_data.split(" ")[5]
                humidity = sensors_data.split(" ")[6]
                gas = sensors_data.split(" ")[7]
                water = sensors_data.split(" ")[8]
                fire = sensors_data.split(" ")[9]
                vibrate = sensors_data.split(" ")[10]
                data_last = sensors_data  # 查验重复

                sensors_sql = "insert into smartcare(entrance,seat,window,pipe,time,light,temperature,humidity,gas,water,fire,vibrate) values('" + entrance + "','" + seat + "','" + window + "','" + pipe + "','" + time.asctime() + "','" + light + "','" + temperature + "','" + humidity + "','" + gas + "','" + water + "','" + fire + "','" + vibrate + "');"  # 字符串拼接成sql语句，语句的功能是“将当前传感器的检测结果存入数据库”
                cursor.execute(sensors_sql)  # 执行该sql语句
                con.commit()  # 提交

            if data_str != '':  # 当不只含有传感器数据，还含有定位数据
                locate_date = data_str.split(" ")[0]
                locate_time = data_str.split(" ")[1]
                locate_city = data_str.split(" ")[2]
                longitude = data_str.split(" ")[3]
                latitude = data_str.split(" ")[4]

                route_sql = "insert into route(locate_date,locate_time,locate_city,longitude,latitude) values('" + locate_date + "','" + locate_time + "','" + locate_city + "','" + longitude + "','" + latitude + "');"
                cursor.execute(route_sql)
                con.commit()





def sensors(request):  # 读取串口数据，并存入数据库
    t = Acquire()
    t.start()
    return HttpResponse("开始存储传感器数据，返回干你自己的事吧")


def index(request):
    return render(request, 'login.html')  # 渲染login.html页面并跳转过去


def login_submit(request):  # 提交用户名和密码，访问数据库判断正确
    if request.POST:  # 若前端页面上有输入的数据
        username = request.POST.get('username')  # 获取网页上输入在 username 文本框内的数据
        userpassword = request.POST.get('userpassword')  # 获取网页上输入在 userpassword 文本框内的数据

        con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
        cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
        sql = "select password from django.index_user where username='" + username + "';"  # 字符串拼接成sql语句，语句的功能是“在用户表内搜索该账号对应的密码”
        try:  # 如果该账号存在：
            cursor.execute(sql)  # 执行该语句
            resule = cursor.fetchone()  # 在搜索到的结果里获取第一个结果（因为账号不能重名，所以找到即为唯一，故没有必要用fetchall，是为了节省数据库在找到唯一的一个账号后继续查找的时间）
            cursor.close()  # 关闭与数据库的连接
            if resule[0] == userpassword:  # 提取数据库返回结果中的真的密码，与网页上输入的密码相对比，若相等
                return render(request, 'admin.html')  # 则成功进入管理员页面
            else:  # 若密码不对
                return HttpResponse("密码错误")  # 则返回密码错误
        except TypeError:  # 如果该账户不存在：
            return HttpResponse("用户不存在")  # 则返回用户不存在

    return render_to_response('login.html', RequestContext(request))  # 若前端页面上没有输入的数据，则重新渲染该页面


def signin(request):
    return render(request, 'signin.html')


def signin_submit(request):
    if request.POST:
        username = request.POST.get("username")
        userpassword = request.POST.get("userpassword")

        con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
        cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
        sql = "insert into django.index_user(username,password) values('" + username + "','" + userpassword + "');"
        cursor.execute(sql)  # 执行该sql语句
        con.commit()  # 提交

    return HttpResponse("注册成功！")


def search(request):
    return render(request, 'search.html')


def search_by_sensor(request):
    return render(request, 'search_by_sensor.html')


def search_by_time(request):
    return render(request, 'search_by_time.html')


def search_all(request):
    Str = []
    con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
    cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
    cursor.execute(u"use django;")  # 执行语句：使用名叫“django”的这个数据库
    sql = "SELECT * FROM django.smartcare;"
    cursor.execute(sql)  # 执行该语句
    result = cursor.fetchall()  # 搜索到的结果我全都要
    con.commit()  # 提交
    cursor.close()  # 关闭与数据库的连接
    for i in result:  # 遍历所有的结果
        Str.append(i)  # 每当追加一条数据时，
        Str.append('<br>')  # 就紧跟着一个html的换行符<br>
    return HttpResponse(Str)


def search_time_submit(request):
    Str = []  # 新建一个空列表（就是一个无限长度的数组）
    Time = request.POST.get('time')  # 获取网页上输入在 time 文本框内的数据
    con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
    cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
    cursor.execute(u"use django;")  # 执行语句：使用名叫“django”的这个数据库
    sql = "select * FROM django.smartcare WHERE time='" + str(Time) + "';"  # 字符串拼接成sql语句，语句的功能是“从kitchencare表中选择 当 网页上输入的Time 与 数据库内time 相同的 行 的所有信息”
    cursor.execute(sql)  # 执行该语句
    result = cursor.fetchall()  # 搜索到的结果我全都要
    con.commit()  # 提交
    cursor.close()  # 关闭与数据库的连接

    # “格式化”
    # 对搜索到的全部结果，在每一条数据后面追加一个html语言的换行符<br>，用于在网页上一行只显示一条数据
    for i in result:  # 遍历所有的结果
        Str.append(i)  # 每当追加一条数据时，
        Str.append('<br>')  # 就紧跟着一个html的换行符<br>

    return HttpResponse(Str)  # 这样显示起来就很工整了


def search_entrance_submit(request):
    Str = []
    entrance = request.POST.get('entrance')  # 获取网页上输入在 time 文本框内的数据
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE entrance='" + str(entrance) + "';"  # 字符串拼接成sql语句，语句的功能是“从kitchencare表中选择 当 网页上输入的entrance 与 数据库内entrance 相同的 行 的所有信息”
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_seat_submit(request):
    Str = []
    seat = request.POST.get('seat')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE seat='" + str(seat) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_window_submit(request):
    Str = []
    window = request.POST.get('window')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE window='" + str(window) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_pipe_submit(request):
    Str = []
    pipe = request.POST.get('pipe')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE pipe='" + str(pipe) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_light_submit(request):
    Str = []
    light = request.POST.get('light')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE light='" + str(light) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_temperature_submit(request):
    Str = []
    temperature = request.POST.get('temperature')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE temperature='" + str(temperature) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_humidity_submit(request):
    Str = []
    humidity = request.POST.get('humidity')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE humidity='" + str(humidity) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_smoke_submit(request):
    Str = []
    smoke = request.POST.get('smoke')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE gas='" + str(smoke) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_water_submit(request):
    Str = []
    water = request.POST.get('water')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE water='" + str(water) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_fire_submit(request):
    Str = []
    fire = request.POST.get('fire')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE fire='" + str(fire) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def search_vibrate_submit(request):
    Str = []
    vibrate = request.POST.get('vibrate')
    con = mysql.connector.connect(user='root', password='root', host='localhost')
    cursor = con.cursor()
    cursor.execute(u"use django;")
    sql = "select * FROM django.smartcare WHERE vibrate='" + str(vibrate) + "';"
    cursor.execute(sql)
    result = cursor.fetchall()
    con.commit()
    cursor.close()

    for i in result:
        Str.append(i)
        Str.append('<br>')

    return HttpResponse(Str)


def control_mode(request):
    return render(request, 'control_mode.html')


def automatic(request):
    ser = serial.Serial()  # 初始化串口对象
    ser.baudrate = 9600  # 设定串口波特率
    ser.port = 'COM5'  # 设定串口名
    ser.open()  # 设定完成，打开串口

    ser.write("1".encode("UTF-8"))  # 先祭奠一个
    time.sleep(1)
    ser.write("on".encode("UTF-8"))
    ser.close()
    return render(request, 'control_mode.html')


def manual(request):
    return render(request, 'manual.html')


def control_submit(cmd):
    ser = serial.Serial()  # 初始化串口对象
    ser.baudrate = 9600  # 设定串口波特率
    ser.port = 'COM5'  # 设定串口名
    ser.open()  # 设定完成，打开串口

    ser.write("1".encode("UTF-8"))  # 先祭奠一个
    time.sleep(1)
    ser.write(cmd.encode("UTF-8"))
    ser.close()


def stack(str):
    cmd = ""
    global flag_r
    global flag_g
    global flag_b
    global servo
    if str == "lona":
        flag_r = True
    elif str == "lofa":
        flag_r = False
    elif str == "lonb":
        flag_g = True
    elif str == "lofb":
        flag_g = False
    elif str == "lonc":
        flag_b = True
    elif str == "lofc":
        flag_b = False
    elif str == "won":
        servo = True
    elif str == "wof":
        servo = False

    if flag_r == True:
        cmd = cmd + "aa"
    if flag_g == True:
        cmd = cmd + "bb"
    if flag_b == True:
        cmd = cmd + "cc"
    if servo == True:
        cmd = cmd + "dd"

    control_submit(cmd)


def turn_light_on_1(request):
    stack("lona")
    return render(request, 'manual.html')


def turn_light_off_1(request):
    stack("lofa")
    return render(request, 'manual.html')


def turn_light_on_2(request):
    stack("lonb")
    return render(request, 'manual.html')


def turn_light_off_2(request):
    stack("lofb")
    return render(request, 'manual.html')


def turn_light_on_3(request):
    stack("lonc")
    return render(request, 'manual.html')


def turn_light_off_3(request):
    stack("lofc")
    return render(request, 'manual.html')


def turn_window_on(request):
    stack("won")
    return render(request, 'manual.html')


def turn_window_off(request):
    stack("wof")
    return render(request, 'manual.html')



def DailyScan():
    sleep_flag_1 = False
    sleep_flag_2 = False
    sleeping = False
    sleeping_time = ""

    getup_flag_1 = False
    getup_flag_2 = False
    getup = True
    getup_time = ""

    cooking_flag_1 = False
    cooking_flag_2 = False
    cooking = False
    cooking_time = ""

    out_time = ""
    in_time = ""

    con = mysql.connector.connect(user='root', password='root', host='localhost')  # 初始化数据库连接器，连接数据库
    cursor = con.cursor()  # 初始化游标，用于执行对数据库的各种操作
    cursor.execute(u"use django;")  # 执行语句：使用名叫“django”的这个数据库




    # entrance = "noPerson"
    # seat = "noPress"
    # window = "close"
    # pipe = "static"
    # light = "bright"
    # temperature = "temperatureNormally"
    # humidity = "humidityNormally"
    # gas = "gasSafe"
    # water = "waterSafe"
    # fire = "fireSafe"
    # vibrate = "stable"
    #
    # sensors_sql = "insert into daily(entrance,seat,window,pipe,time,light,temperature,humidity,gas,water,fire,vibrate) values('" + entrance + "','" + seat + "','" + window + "','" + pipe + "','" + time.strftime("%Y-%m-%d %H:%M:%S", time.localtime()) + "','" + light + "','" + temperature + "','" + humidity + "','" + gas + "','" + water + "','" + fire + "','" + vibrate + "');"  # 字符串拼接成sql语句，语句的功能是“将当前传感器的检测结果存入数据库”
    # cursor.execute(sensors_sql)  # 执行该sql语句
    # con.commit()  # 提交



    sql = "SELECT * FROM django.daily;"
    cursor.execute(sql)  # 执行该语句

    try:
        Str = cursor.next()
        while Str != None:
            # print(Str)

            if sleeping is False:
                if Str[1] == "Press":
                    sleep_flag_1 = True
                if sleep_flag_1 is True & (Str[5] == "dark"):
                    sleep_flag_2 = True
                if sleep_flag_2 is True & (Str[6] != "temperatureNormally"):
                    sleep_flag_1 = False
                    sleep_flag_2 = False
                    sleeping = True
                    getup = False
                    sleeping_time = Str[4]

            if getup is False:
                if Str[1] == "noPress":
                    getup_flag_1 = True
                if getup_flag_1 is True & (Str[5] == "bright"):
                    getup_flag_2 = True
                if getup_flag_2 is True & (Str[6] == "temperatureNormally"):
                    getup_flag_1 = False
                    getup_flag_2 = False
                    getup = True
                    sleeping = False
                    getup_time = Str[4]

            if cooking is False:
                if Str[3] == "flow":
                    cooking_flag_1 = True
                if cooking_flag_1 is True & (Str[8] == "leak"):
                    cooking_flag_2 = True
                if cooking_flag_2 is True & (Str[10] == "burn"):
                    cooking_flag_1 = False
                    cooking_flag_2 = False
                    cooking = True
                    cooking_time = Str[4]

            if Str[0] == "Out":
                out_time = Str[4]

            if Str[0] == "In":
                in_time = Str[4]

            Str = cursor.next()

    except Exception:
        pass

    print(sleeping_time)
    print(getup_time)
    print(cooking_time)
    print(out_time)
    print(in_time)

    with open("sleep.csv", 'a', newline='') as f:
        row = [sleeping_time, int(sleeping_time.split(" ")[1].split(":")[0])*3600+int(sleeping_time.split(" ")[1].split(":")[1])*60+int(sleeping_time.split(" ")[1].split(":")[2])]
        write = csv.writer(f)
        write.writerow(row)

    with open("getup.csv", 'a', newline='') as f:
        row = [getup_time, int(getup_time.split(" ")[1].split(":")[0])*3600+int(getup_time.split(" ")[1].split(":")[1])*60+int(getup_time.split(" ")[1].split(":")[2])]
        write = csv.writer(f)
        write.writerow(row)

    with open("cooking.csv", 'a', newline='') as f:
        row = [cooking_time, int(cooking_time.split(" ")[1].split(":")[0])*3600+int(cooking_time.split(" ")[1].split(":")[1])*60+int(cooking_time.split(" ")[1].split(":")[2])]
        write = csv.writer(f)
        write.writerow(row)

    with open("out.csv", 'a', newline='') as f:
        row = [out_time, int(out_time.split(" ")[1].split(":")[0])*3600+int(out_time.split(" ")[1].split(":")[1])*60+int(out_time.split(" ")[1].split(":")[2])]
        write = csv.writer(f)
        write.writerow(row)

    with open("in.csv", 'a', newline='') as f:
        row = [in_time, int(in_time.split(" ")[1].split(":")[0])*3600+int(in_time.split(" ")[1].split(":")[1])*60+int(in_time.split(" ")[1].split(":")[2])]
        write = csv.writer(f)
        write.writerow(row)

    # clear_sql = "truncate table django.daily;"
    # cursor.execute(clear_sql)  # 清空 daily 表


def forecast(str):
    dataframe = pandas.read_csv("C:/Users/54961/Desktop/hello/csv/" + str + ".csv")
    sleep = dataframe[0:25]
    test = dataframe[25:]

    dataframe['Timestamp'] = pandas.to_datetime(dataframe['Datetime'], format='%Y/%m/%d %H:%M')
    dataframe.index = dataframe['Timestamp']
    dataframe = dataframe.resample('D').mean()

    sleep['Timestamp'] = pandas.to_datetime(sleep['Datetime'], format='%Y/%m/%d %H:%M')
    sleep.index = sleep['Timestamp']
    sleep = sleep.resample('D').mean()

    test['Timestamp'] = pandas.to_datetime(test['Datetime'], format='%Y/%m/%d %H:%M')
    test.index = test['Timestamp']
    test = test.resample('D').mean()

    avg = test.copy()

    fit = ExponentialSmoothing(np.asarray(sleep['B']), seasonal_periods=7, trend='add', seasonal='add').fit()
    avg['H_W'] = fit.forecast(len(test))

    plt.figure(figsize=(16, 8))
    plt.plot(sleep['B'], label=str)
    plt.plot(test['B'], label='test')
    plt.plot(avg['H_W'], label='Forecast')
    plt.legend(loc='best')
    plt.savefig("C:/Users/54961/Desktop/hello/pic/forecast_" + str + ".png")
    Image.open("C:/Users/54961/Desktop/hello/pic/forecast_" + str + ".png").convert("RGB").save("C:/Users/54961/Desktop/hello/static/forecast_" + str + ".jpg")


def Forecast(request):
    forecast("sleep")
    forecast("getup")
    forecast("cooking")
    forecast("in")
    forecast("out")
    return render(request, 'forecast.html')
