"""hello URL Configuration
The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/2.0/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.urls import path
from helloworld import views

# 将从浏览器进入的网页（前端）映射到相应的后台（业务处理逻辑文件views.py）

urlpatterns = [
    path('', views.index),

    path(r'login_submit', views.login_submit),
    path(r'signin', views.signin),
    path(r'signin_submit', views.signin_submit),

    path(r'sensor', views.sensors),

    path(r'search', views.search),
    path(r'search_by_sensor', views.search_by_sensor),

    path(r'search_entrance_submit', views.search_entrance_submit),
    path(r'search_seat_submit', views.search_seat_submit),
    path(r'search_window_submit', views.search_window_submit),
    path(r'search_pipe_submit', views.search_pipe_submit),
    path(r'search_light_submit', views.search_light_submit),
    path(r'search_temperature_submit', views.search_temperature_submit),
    path(r'search_humidity_submit', views.search_humidity_submit),
    path(r'search_smoke_submit', views.search_smoke_submit),
    path(r'search_water_submit', views.search_water_submit),
    path(r'search_fire_submit', views.search_fire_submit),
    path(r'search_vibrate_submit', views.search_vibrate_submit),
    path(r'search_by_time', views.search_by_time),
    path(r'search_time_submit', views.search_time_submit),
    path(r'search_all', views.search_all),

    path(r'control_mode', views.control_mode),
    path(r'automatic', views.automatic),
    path(r'manual', views.manual),
    path(r'turn_light_on_1', views.turn_light_on_1),
    path(r'turn_light_off_1', views.turn_light_off_1),
    path(r'turn_light_on_2', views.turn_light_on_2),
    path(r'turn_light_off_2', views.turn_light_off_2),
    path(r'turn_light_on_3', views.turn_light_on_3),
    path(r'turn_light_off_3', views.turn_light_off_3),
    path(r'turn_window_on', views.turn_window_on),
    path(r'turn_window_off', views.turn_window_off),

    path(r'frequent_location_number', views.frequent_location_number),
    path(r'forecast', views.Forecast),
    path(r'analyse_cluster', views.analyse_cluster)
]
