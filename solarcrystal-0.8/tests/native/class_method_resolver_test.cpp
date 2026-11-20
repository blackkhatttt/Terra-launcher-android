#include "class_method_resolver.h"
#include <cassert>
#include <cstdint>
#include <fstream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <vector>
using namespace solarcrystal;
static ClassFileModel load_model(const char* path) {
 std::ifstream input(path,std::ios::binary);
 if(!input.good()) throw std::runtime_error(std::string("Cannot open class: ")+path);
 const std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(input)),{});
 if(bytes.empty()) throw std::runtime_error("Class file is empty");
 return ClassFileModel::parse(bytes.data(),bytes.size());
}
int main(int argc,char** argv) {
 assert(argc==4);
 const auto base=load_model(argv[1]);
 const auto child=load_model(argv[2]);
 const auto iface=load_model(argv[3]);
 ClassFileRepository repository;
 repository.add(base); repository.add(child); repository.add(iface);
 ClassMethodResolver resolver(repository);
 const auto inherited=resolver.resolve_virtual_method(child.name(),"inheritedMethod","()I");
 assert(inherited.method && inherited.declaring_class==&base);
 assert(inherited.method->has_code && !inherited.from_interface);
 const auto default_method=resolver.resolve_virtual_method(child.name(),"interfaceDefaultMethod","()I");
 assert(default_method.method && default_method.declaring_class==&iface);
 assert(default_method.method->has_code && default_method.from_interface);
 const auto interface_lookup=resolver.resolve_interface_method(iface.name(),"interfaceDefaultMethod","()I");
 assert(interface_lookup.method && interface_lookup.declaring_class==&iface);
 assert(interface_lookup.from_interface);
 const auto missing=resolver.resolve_virtual_method(child.name(),"missingMethod","()I");
 assert(!missing.method && !missing.declaring_class);
 bool rejected=false;
 try { (void)resolver.resolve_interface_method(base.name(),"inheritedMethod","()I"); }
 catch(const std::runtime_error&) { rejected=true; }
 assert(rejected);
}
