#include "class_file_linkage_resolver.h"
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
 if(bytes.empty()) throw std::runtime_error("Empty class file");
 return ClassFileModel::parse(bytes.data(),bytes.size());
}
int main(int argc,char** argv) {
 assert(argc==4);
 const auto base=load_model(argv[1]);
 const auto child=load_model(argv[2]);
 const auto iface=load_model(argv[3]);
 ClassFileRepository repository; repository.add(base); repository.add(child); repository.add(iface);
 ClassFileLinkageResolver resolver(repository);
 bool field=false, inherited_method=false, interface_method=false;
 for(std::size_t i=1;i<child.constant_pool().size();++i) {
  const auto tag=child.constant_pool()[i].tag;
  if(tag!=9 && tag!=10 && tag!=11) continue;
  const auto ref=child.member_reference_at(static_cast<std::uint16_t>(i));
  const auto resolved=resolver.resolve(child,static_cast<std::uint16_t>(i));
  if(ref.name=="sharedValue" && tag==9) {
   assert(resolved.is_field && resolved.declaring_class==base.name()); field=true;
  }
  if(ref.name=="inheritedMethod" && (tag==10 || tag==11)) {
   assert(!resolved.is_field && resolved.declaring_class==base.name()); inherited_method=true;
  }
  if(ref.name=="interfaceDefaultMethod" && (tag==10 || tag==11)) {
   assert(!resolved.is_field && resolved.from_interface);
   assert(resolved.declaring_class==iface.name()); interface_method=true;
  }
 }
 assert(field && inherited_method && interface_method);
}
