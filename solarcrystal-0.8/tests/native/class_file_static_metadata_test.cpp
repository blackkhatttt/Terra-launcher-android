#include "class_file_model.h"
#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <vector>
using namespace solarcrystal;
int main(int argc,char** argv) {
 if(argc!=2) throw std::runtime_error("Expected static fixture class path");
 std::ifstream in(argv[1],std::ios::binary); if(!in) throw std::runtime_error("Could not open static fixture");
 std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(in)),{});
 const auto model=ClassFileModel::parse(bytes.data(),bytes.size());
 assert(model.name()=="SolarCrystalStaticFixture");
 bool magic=false,big=false,label=false,counter=false,computed=false;
 for(const auto& f:model.fields()) {
  if(f.name=="MAGIC"){magic=true;assert(f.is_static()&&f.has_constant_value());const auto& v=model.constant_value(f);assert(v.tag==3&&v.bits==731u);}
  if(f.name=="BIG_MAGIC"){big=true;assert(f.is_static()&&f.has_constant_value());const auto& v=model.constant_value(f);assert(v.tag==5&&v.bits==0x1020304050607080ULL);}
  if(f.name=="LABEL"){label=true;assert(f.is_static()&&f.has_constant_value());const auto& v=model.constant_value(f);assert(v.tag==8);}
  if(f.name=="counter"){counter=true;assert(f.is_static()&&!f.has_constant_value());}
  if(f.name=="computed"){computed=true;assert(f.is_static()&&!f.has_constant_value());}
 }
 assert(magic&&big&&label&&counter&&computed);
 const auto* init=model.class_initializer();assert(init&&init->has_code&&init->name=="<clinit>"&&init->descriptor=="()V");
 const auto* read=model.find_method("readCounter","()I");assert(read&&read->has_code);
 std::cout<<"SolarCrystal 0.8.9b static field metadata: PASS\n";
 std::cout<<"ConstantValue, numeric constant bits, static fields and <clinit> discovery: PASS\n";
}
