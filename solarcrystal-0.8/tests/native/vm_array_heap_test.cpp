#include "array_bytecode_executor.h"
#include <cassert>
#include <iostream>
#include <stdexcept>
using namespace solarcrystal;
template<class F>bool fails(F f){try{f();}catch(const std::runtime_error&){return true;}return false;}
int main(){VmArrayHeap arrays;VmFrame f(2,8);f.push(VmValue::integer(3));auto r=execute_array_bytecode({0xbc,10},f,arrays);auto id=f.pop().as_reference();assert(arrays.length(id)==3&&r.arrays_allocated==1);arrays.put_int(id,1,42);assert(arrays.get_int(id,1)==42);VmFrame read(1,4);read.push(VmValue::reference(id));read.push(VmValue::integer(1));auto rr=execute_array_bytecode({0x2e},read,arrays);assert(read.pop().as_integer()==42&&rr.elements_read==1);VmFrame len(1,2);len.push(VmValue::reference(id));execute_array_bytecode({0xbe},len,arrays);assert(len.pop().as_integer()==3);
VmFrame refs(1,6);refs.push(VmValue::integer(2));execute_array_bytecode({0xbd,0,5},refs,arrays,[](std::uint16_t cp){assert(cp==5);return std::string("java/lang/Object");});auto rid=refs.pop().as_reference();assert(arrays.get_reference(rid,0).is_null());refs.push(VmValue::reference(rid));refs.push(VmValue::integer(1));refs.push(VmValue::reference(7));execute_array_bytecode({0x53},refs,arrays);assert(arrays.get_reference(rid,1).as_reference()==7);assert(fails([&]{arrays.get_int(id,3);}));assert(fails([&]{arrays.new_int_array(-1);}));assert(fails([&]{VmFrame x(1,2);x.push(VmValue::integer(1));execute_array_bytecode({0xbc,8},x,arrays);}));std::cout<<"SolarCrystal 0.8.8a array heap and bytecodes: PASS\n";}
