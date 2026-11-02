cmake_minimum_required(VERSION 3.22.1)
project(nexus_native)

add_library(nexus_native SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(nexus_native ${log-lib})
