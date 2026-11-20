#include "reference_bytecode_executor.h"
#include <cassert>
#include <cstdint>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>
using namespace solarcrystal;
template<class F> bool throws_runtime_error(F f){try{f();}catch(const std::runtime_error&){return true;}return false;}
int main(){
 ObjectHeap heap; VmFrame frame(4,8);
 const std::vector<std::uint8_t> allocation={0xbb,0,7,0x59,0x4b,0x2a,0x4c};
 auto basic=execute_reference_bytecode(allocation,frame,heap,[](std::uint16_t i){assert(i==7);return std::string("SolarCrystalNode");});
 assert(basic.instructions_executed==5&&basic.objects_allocated==1);
 assert(frame.get_local(0).as_reference()==frame.get_local(1).as_reference());assert(frame.pop().as_reference()==frame.get_local(0).as_reference());
 VmFrame caller(1,4);const std::vector<std::uint8_t> initcall={0xbb,0,7,0x59,0xb7,0,9,0x4b};
 const std::vector<std::uint8_t> initcode={0x2a,0x10,0x2a,0xb5,0,11,0xb1};
 auto built=execute_reference_bytecode(initcall,caller,heap,[](std::uint16_t){return std::string("SolarCrystalNode");},
 [&](std::uint16_t){return SpecialMethod{"SolarCrystalNode","<init>","()V",initcode,1,3};},
 [](std::uint16_t){return InstanceField{"SolarCrystalNode","value"};});
 assert(built.objects_allocated==1&&built.constructors_invoked==1&&built.fields_written==1);
 auto id=caller.get_local(0).as_reference();assert(heap.get_int(id,"SolarCrystalNode","value")==42);
 VmFrame reader(1,2);reader.set_local(0,VmValue::reference(id));
 auto read=execute_reference_bytecode({0x2a,0xb4,0,12},reader,heap,[](std::uint16_t){return std::string();},{},
 [](std::uint16_t){return InstanceField{"SolarCrystalNode","value"};});
 assert(read.fields_read==1&&reader.pop().as_integer()==42);
 // Same frame contains a reference, an integer local, and an integer return.
 VmFrame mixed(3,8);mixed.set_local(0,VmValue::reference(id));
 auto mr=execute_reference_bytecode({0x2a,0xb4,0,12,0x04,0x60,0x3c,0x1b,0xac},mixed,heap,
 [](std::uint16_t){return std::string();},{},[](std::uint16_t){return InstanceField{"SolarCrystalNode","value"};});
 assert(mr.returned&&mr.has_return_value&&mr.return_value.as_integer()==43&&mixed.get_local(1).as_integer()==43);
 VmFrame arithmetic(2,4);
 auto ar=execute_reference_bytecode({0x10,10,0x10,2,0x68,0x3b,0x1a,0x04,0x60,0xac},arithmetic,heap,[](std::uint16_t){return std::string();});
 assert(ar.return_value.as_integer()==21);
 VmFrame base(1,3);auto br=execute_reference_bytecode({0xbb,0,1,0x59,0xb7,0,2,0x4b},base,heap,
 [](std::uint16_t){return std::string("Example");},[](std::uint16_t){return SpecialMethod{"java/lang/Object","<init>","()V",{},1,1};});
 assert(br.constructors_invoked==1);
 VmFrame nulls(2,3);nulls.set_local(0,VmValue::null_reference());
 (void)execute_reference_bytecode({0x19,0,0x3a,1},nulls,heap,[](std::uint16_t){return std::string();});assert(nulls.get_local(1).is_null());
 assert(throws_runtime_error([&]{VmFrame f(1,2);(void)execute_reference_bytecode({0xbb,0,1,0x57},f,heap,[](std::uint16_t){return std::string("X");},{},{},1);}));
 assert(throws_runtime_error([&]{VmFrame f(1,2);(void)execute_reference_bytecode({0xbb,0,1,0x59,0xb7,0,2},f,heap,[](std::uint16_t){return std::string("X");},[](std::uint16_t){return SpecialMethod{"Other","<init>","()V",{},1,1};});}));
 std::cout<<"SolarCrystal 0.8.5 unified typed-frame bytecodes: PASS\n";
 std::cout<<"integer locals/arithmetic, references, constructors, fields and returns: PASS\n";
}
