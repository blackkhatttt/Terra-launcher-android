#include "class_file_execution_resolver.h"
#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <vector>
using namespace solarcrystal;
static ClassFileModel load_model(const char* path) {
 std::ifstream input(path,std::ios::binary);
 if(!input.good()) throw std::runtime_error(std::string("Cannot open class file: ")+path);
 const std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(input)),std::istreambuf_iterator<char>());
 if(bytes.empty()) throw std::runtime_error("Class file is empty");
 return ClassFileModel::parse(bytes.data(),bytes.size());
}
int main(int argc,char** argv) {
 assert(argc==3);
 const auto base=load_model(argv[1]); const auto child=load_model(argv[2]);
 assert(base.name()=="SolarCrystalInheritedBaseFixture");
 assert(child.name()=="SolarCrystalInheritedChildFixture");
 assert(child.super_name()==base.name());
 ClassFileRepository repository; repository.add(base); repository.add(child);
 ClassFileExecutionResolver resolver(child,repository);
 const auto resolve_field=resolver.instance_field_resolver();
 std::uint16_t cp=0;
 for(std::size_t i=1;i<child.constant_pool().size();++i) {
  if(child.constant_pool()[i].tag!=9) continue;
  const auto ref=child.member_reference_at(static_cast<std::uint16_t>(i));
  if(ref.owner==child.name()&&ref.name=="inheritedValue"&&ref.descriptor=="I") {cp=static_cast<std::uint16_t>(i);break;}
 }
 assert(cp!=0);
 const auto field=resolve_field(cp);
 assert(field.name=="inheritedValue"&&field.access_class==child.name());
 assert(field.compatible_runtime_classes.size()==1&&field.compatible_runtime_classes[0]==child.name());
 ObjectHeap heap; const auto id=heap.allocate(child.name(),{});
 const auto* writer=child.find_method("writeInherited","(I)V");
 const auto* reader=child.find_method("readInherited","()I");
 assert(writer&&writer->has_code&&reader&&reader->has_code);
 VmFrame wf(writer->max_locals,writer->max_stack);
 wf.set_local(0,VmValue::reference(id));wf.set_local(1,VmValue::integer(73));
 const auto wr=execute_reference_bytecode(writer->code,wf,heap,resolver.new_class_resolver(),resolver.special_method_resolver(),resolve_field);
 assert(wr.returned&&wr.fields_written==1);
 VmFrame rf(reader->max_locals,reader->max_stack);rf.set_local(0,VmValue::reference(id));
 const auto rr=execute_reference_bytecode(reader->code,rf,heap,resolver.new_class_resolver(),resolver.special_method_resolver(),resolve_field);
 assert(rr.returned&&rr.has_return_value&&rr.return_value.as_integer()==73&&rr.fields_read==1);
 std::cout<<"SolarCrystal 0.8.7b inherited integer fields: PASS\n";
 std::cout<<"child bytecode reads and writes parent-declared field: PASS\n";
}
