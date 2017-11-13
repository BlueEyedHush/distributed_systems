cmake_minimum_required(VERSION 3.3)
project(server)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c11")

set(SOURCE_FILES main.c)
add_executable(server ${SOURCE_FILES})