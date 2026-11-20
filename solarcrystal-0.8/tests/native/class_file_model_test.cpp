#include "class_file_model.h"
#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <vector>
using namespace solarcrystal;
int main(int argc,char** argv){
 assert(argc==2);std::ifstream in(argv[1],std::ios::binary);assert(in.good());
 std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(in)),std::istreambuf_iterator<char>());assert(!bytes.empty());
 auto model=ClassFileModel::parse(bytes.data(),bytes.size());
 assert(model.name()=="SolarCrystalObjectFixture");assert(model.super_name()=="java/lang/Object");
 auto ctor=model.find_method("<init>","()V");assert(ctor&&ctor->has_code&&!ctor->code.empty()&&ctor->max_locals>=1);
 auto write=model.find_method("writeValue","(I)V");assert(write&&write->has_code);
 auto read=model.find_method("readValue","()I");assert(read&&read->has_code);
 bool field=false,object_ctor=false;
 for(std::uint16_t i=1;i<model.constant_pool().size();++i){auto tag=model.constant_pool()[i].tag;
  if(tag==9){auto ref=model.member_reference_at(i);if(ref.owner==model.name()&&ref.name=="value"&&ref.descriptor=="I")field=true;}
  if(tag==10||tag==11){auto ref=model.member_reference_at(i);if(ref.owner=="java/lang/Object"&&ref.name=="<init>"&&ref.descriptor=="()V")object_ctor=true;}
 }
 assert(field&&object_ctor);bool rejected=false;try{const std::vector<std::uint8_t> bad={0,1,2,3};(void)ClassFileModel::parse(bad.data(),bad.size());}catch(const std::runtime_error&){rejected=true;}assert(rejected);
 std::cout<<"SolarCrystal 0.8.6a class-file model: PASS\n";
 std::cout<<"constant pool, Code attributes, methods, fields and member references: PASS\n";
}