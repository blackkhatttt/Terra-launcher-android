#include "array_bytecode_executor.h"
#include <stdexcept>
#include <string>
namespace solarcrystal { namespace {
std::uint8_t u1(const std::vector<std::uint8_t>&c,std::size_t&p){if(p>=c.size())throw std::runtime_error("Truncated array bytecode");return c[p++];}
std::uint16_t u2(const std::vector<std::uint8_t>&c,std::size_t&p){auto a=u1(c,p);return static_cast<std::uint16_t>((a<<8)|u1(c,p));}
std::int32_t pop_int(VmFrame&f){return f.pop().as_integer();}
ObjectId pop_array(VmFrame&f,const VmArrayHeap&a){auto v=f.pop();if(!v.is_reference()||!a.is_array(v.as_reference()))throw std::runtime_error("Array operation requires array reference");return v.as_reference();}
}}
ArrayExecutionResult execute_array_bytecode(const std::vector<std::uint8_t>&c,VmFrame&f,VmArrayHeap&a,const NewArrayClassResolver&r,std::size_t budget){if(!budget)throw std::runtime_error("Array instruction budget is zero");ArrayExecutionResult out;std::size_t p=0;while(p<c.size()){if(++out.instructions_executed>budget)throw std::runtime_error("Array instruction budget exceeded");auto op=u1(c,p);switch(op){
case 0xbc:{auto type=u1(c,p);auto n=pop_int(f);if(type!=10)throw std::runtime_error("Only int newarray (atype 10) supported");auto id=a.new_int_array(n);f.push(VmValue::reference(id));++out.arrays_allocated;break;}
case 0xbd:{auto cp=u2(c,p);auto n=pop_int(f);if(!r)throw std::runtime_error("anewarray has no class resolver");auto name=r(cp);auto id=a.new_reference_array(n,name);f.push(VmValue::reference(id));++out.arrays_allocated;break;}
case 0x2e:{auto i=pop_int(f);auto id=pop_array(f,a);f.push(VmValue::integer(a.get_int(id,i)));++out.elements_read;break;}
case 0x4f:{auto v=pop_int(f),i=pop_int(f);auto id=pop_array(f,a);a.put_int(id,i,v);++out.elements_written;break;}
case 0x32:{auto i=pop_int(f);auto id=pop_array(f,a);f.push(a.get_reference(id,i));++out.elements_read;break;}
case 0x53:{auto v=f.pop();auto i=pop_int(f);auto id=pop_array(f,a);a.put_reference(id,i,v);++out.elements_written;break;}
case 0xbe:{auto id=pop_array(f,a);f.push(VmValue::integer(static_cast<std::int32_t>(a.length(id))));break;}
default:throw std::runtime_error("Unsupported array bytecode opcode: "+std::to_string(op));
}}return out;}
}