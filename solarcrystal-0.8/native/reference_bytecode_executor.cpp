#include "reference_bytecode_executor.h"
#include <cstdint>
#include <limits>
#include <stdexcept>
#include <string>
namespace solarcrystal { namespace {
struct Context {
 ObjectHeap& heap; const NewClassResolver& new_class;
 const SpecialMethodResolver& special; const InstanceFieldResolver& field;
 std::size_t budget; ReferenceExecutionResult result; std::size_t constructor_depth=0;
};
std::uint8_t u1(const std::vector<std::uint8_t>& c,std::size_t& p){if(p>=c.size())throw std::runtime_error("Truncated bytecode operand");return c[p++];}
std::uint16_t u2(const std::vector<std::uint8_t>& c,std::size_t& p){auto h=u1(c,p);return static_cast<std::uint16_t>((h<<8)|u1(c,p));}
std::int32_t add(std::int32_t a,std::int32_t b){return static_cast<std::int32_t>(std::uint32_t(a)+std::uint32_t(b));}
std::int32_t sub(std::int32_t a,std::int32_t b){return static_cast<std::int32_t>(std::uint32_t(a)-std::uint32_t(b));}
std::int32_t mul(std::int32_t a,std::int32_t b){return static_cast<std::int32_t>(std::uint32_t(a)*std::uint32_t(b));}
std::int32_t pop_i(VmFrame& f){return f.pop().as_integer();}
VmValue load_ref(const VmFrame& f,std::size_t i,const ObjectHeap& h){
 const auto& v=f.get_local(i); if(!v.is_reference()&&!v.is_null())throw std::runtime_error("aload requires reference or null");
 if (v.is_reference() && !h.is_valid(v.as_reference())) {
  throw std::runtime_error("aload invalid reference");
 }
 return v;
}
void store_ref(VmFrame& f,std::size_t i,const VmValue& v,const ObjectHeap& h){
 if(!v.is_reference()&&!v.is_null())throw std::runtime_error("astore requires reference or null");
 if (v.is_reference() && !h.is_valid(v.as_reference())) {
  throw std::runtime_error("astore invalid reference");
 }
 f.set_local(i, v);
}
ObjectId pop_ref(VmFrame& f,const ObjectHeap& h){
 auto v=f.pop(); if(!v.is_reference()||!h.is_valid(v.as_reference()))throw std::runtime_error("Instance operation requires valid non-null reference"); return v.as_reference();
}
void run_code(const std::vector<std::uint8_t>&,VmFrame&,Context&,bool);
void invoke_init(std::uint16_t cp,VmFrame& caller,Context& x){
 if(!x.special)throw std::runtime_error("invokespecial has no method resolver");
 auto m=x.special(cp); if(m.name!="<init>"||m.descriptor!="()V")throw std::runtime_error("Only no-argument void constructors supported");
 auto id=pop_ref(caller,x.heap); auto actual=x.heap.class_name(id);
 if(m.owner_class=="java/lang/Object"){++x.result.constructors_invoked;return;}
 if(m.owner_class!=actual)throw std::runtime_error("Superclass constructor dispatch unsupported");
 if(x.constructor_depth>=16)throw std::runtime_error("Constructor depth limit exceeded");
 VmFrame f(m.max_locals,m.max_stack); f.set_local(0,VmValue::reference(id));
 ++x.constructor_depth; ++x.result.constructors_invoked;
 try{run_code(m.code,f,x,true);}catch(...){--x.constructor_depth;throw;}
 --x.constructor_depth;
}
void run_code(const std::vector<std::uint8_t>& c,VmFrame& f,Context& x,bool ctor){
 std::size_t p=0;
 while(p<c.size()){
  if(++x.result.instructions_executed>x.budget)throw std::runtime_error("Reference bytecode instruction budget exceeded");
  auto op=u1(c,p);
  switch(op){
   case 0x02:case 0x03:case 0x04:case 0x05:case 0x06:case 0x07:case 0x08:f.push(VmValue::integer(std::int32_t(op)-3));break;
   case 0x10:f.push(VmValue::integer(static_cast<std::int8_t>(u1(c,p))));break;
   case 0x15:f.push(VmValue::integer(f.get_local(u1(c,p)).as_integer()));break;
   case 0x1a:case 0x1b:case 0x1c:case 0x1d:f.push(VmValue::integer(f.get_local(op-0x1a).as_integer()));break;
   case 0x36:{auto i=u1(c,p);f.set_local(i,VmValue::integer(pop_i(f)));break;}
   case 0x3b:case 0x3c:case 0x3d:case 0x3e:f.set_local(op-0x3b,VmValue::integer(pop_i(f)));break;
   case 0x60:{auto b=pop_i(f),a=pop_i(f);f.push(VmValue::integer(add(a,b)));break;}
   case 0x64:{auto b=pop_i(f),a=pop_i(f);f.push(VmValue::integer(sub(a,b)));break;}
   case 0x68:{auto b=pop_i(f),a=pop_i(f);f.push(VmValue::integer(mul(a,b)));break;}
   case 0x6c:{auto b=pop_i(f),a=pop_i(f);if(!b)throw std::runtime_error("ArithmeticException: division by zero");f.push(VmValue::integer(a==std::numeric_limits<std::int32_t>::min()&&b==-1?a:a/b));break;}
   case 0x74:f.push(VmValue::integer(sub(0,pop_i(f))));break;
   case 0x19:f.push(load_ref(f,u1(c,p),x.heap));break;
   case 0x2a:case 0x2b:case 0x2c:case 0x2d:f.push(load_ref(f,op-0x2a,x.heap));break;
   case 0x3a:{auto i=u1(c,p);auto v=f.pop();store_ref(f,i,v,x.heap);break;}
   case 0x4b:case 0x4c:case 0x4d:case 0x4e:{auto v=f.pop();store_ref(f,op-0x4b,v,x.heap);break;}
   case 0x59:f.duplicate_top();break; case 0x57:(void)f.pop();break;
   case 0xbb:{auto cp=u2(c,p);if(!x.new_class)throw std::runtime_error("new has no class resolver");auto name=x.new_class(cp);if(name.empty())throw std::runtime_error("new resolved empty class");auto id=x.heap.allocate(name,{});f.push(VmValue::reference(id));++x.result.objects_allocated;break;}
   case 0xb7:invoke_init(u2(c,p),f,x);break;
   case 0xb5:{auto cp=u2(c,p);if(!x.field)throw std::runtime_error("putfield has no field resolver");auto fld=x.field(cp);auto v=f.pop();if(!v.is_integer())throw std::runtime_error("Only integer putfield supported");auto id=pop_ref(f,x.heap);if(fld.owner_class!=x.heap.class_name(id))throw std::runtime_error("putfield owner mismatch");if(!x.heap.has_int_field(id,fld.name))x.heap.define_int_field(id,fld.name,0);x.heap.put_int(id,fld.owner_class,fld.name,v.as_integer());++x.result.fields_written;break;}
   case 0xb4:{auto cp=u2(c,p);if(!x.field)throw std::runtime_error("getfield has no field resolver");auto fld=x.field(cp);auto id=pop_ref(f,x.heap);if(fld.owner_class!=x.heap.class_name(id))throw std::runtime_error("getfield owner mismatch");f.push(VmValue::integer(x.heap.get_int(id,fld.owner_class,fld.name)));++x.result.fields_read;break;}
   case 0xac:if(ctor)throw std::runtime_error("Constructor cannot use ireturn");x.result.return_value=VmValue::integer(pop_i(f));x.result.has_return_value=true;x.result.returned=true;return;
   case 0xb1:x.result.returned=true;return;
   default:throw std::runtime_error("Unsupported reference bytecode opcode: "+std::to_string(op));
  }
 }
}
} // namespace
ReferenceExecutionResult execute_reference_bytecode(const std::vector<std::uint8_t>& code,VmFrame& frame,ObjectHeap& heap,
 const NewClassResolver& new_class,const SpecialMethodResolver& special,const InstanceFieldResolver& field,std::size_t budget){
 if(!budget)throw std::runtime_error("Reference bytecode instruction budget is zero");
 Context x{heap,new_class,special,field,budget,{},0};run_code(code,frame,x,false);return x.result;
}
}
