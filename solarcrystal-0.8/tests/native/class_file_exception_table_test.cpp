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
 if(argc!=2)throw std::runtime_error("Expected compiled fixture class path");
 std::ifstream input(argv[1],std::ios::binary);if(!input)throw std::runtime_error("Could not open exception fixture");
 std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(input)),{});
 const auto model=ClassFileModel::parse(bytes.data(),bytes.size());
 const auto* divide=model.find_method("safeDivide","(II)I");assert(divide&&divide->has_code&&!divide->exception_handlers.empty());
 bool arithmetic=false;for(const auto& e:divide->exception_handlers){assert(e.start_pc<e.end_pc&&e.end_pc<=divide->code.size()&&e.handler_pc<divide->code.size());if(e.catch_class=="java/lang/ArithmeticException")arithmetic=true;}assert(arithmetic);
 const auto* finally_method=model.find_method("withFinally","(I)I");assert(finally_method&&finally_method->has_code);
 bool catch_all=false;for(const auto& e:finally_method->exception_handlers)if(e.catch_type==0&&e.catch_class.empty())catch_all=true;assert(catch_all);
 std::cout<<"SolarCrystal 0.8.9a exception table parsing passed.\n";return 0;
}
