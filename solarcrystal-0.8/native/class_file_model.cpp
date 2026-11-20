#include "class_file_model.h"
#include <limits>
#include <stdexcept>
#include <utility>
namespace solarcrystal { namespace {
constexpr std::size_t kMaxClassBytes=16u*1024u*1024u;
constexpr std::uint32_t kMaxCodeBytes=65535u;
class Reader {
public:
 Reader(const std::uint8_t* d,std::size_t n):data_(d),size_(n){if(!d&&n)throw std::runtime_error("Null class-file buffer");}
 std::uint8_t u1(){require(1);return data_[position_++];}
 std::uint16_t u2(){auto h=static_cast<std::uint16_t>(u1());return static_cast<std::uint16_t>((h<<8)|u1());}
 std::uint32_t u4(){return (std::uint32_t(u1())<<24)|(std::uint32_t(u1())<<16)|(std::uint32_t(u1())<<8)|u1();}
 std::string bytes(std::size_t n){require(n);std::string v(reinterpret_cast<const char*>(data_+position_),n);position_+=n;return v;}
 std::vector<std::uint8_t> byte_vector(std::size_t n){require(n);std::vector<std::uint8_t> v(data_+position_,data_+position_+n);position_+=n;return v;}
 void skip(std::size_t n){require(n);position_+=n;}
 std::size_t remaining()const{return size_-position_;}
private:
 const std::uint8_t* data_;std::size_t size_,position_=0;
 void require(std::size_t n)const{if(n>size_-position_)throw std::runtime_error("Unexpected end of class file");}
};
std::string cp_utf8(const std::vector<ConstantPoolEntry>& cp,std::uint16_t i){
 if(!i||i>=cp.size()||cp[i].tag!=1)throw std::runtime_error("Invalid CONSTANT_Utf8 reference");return cp[i].text;
}
std::string cp_class(const std::vector<ConstantPoolEntry>& cp,std::uint16_t i){
 if(!i||i>=cp.size()||cp[i].tag!=7)throw std::runtime_error("Invalid CONSTANT_Class reference");return cp_utf8(cp,cp[i].first);
}
void skip_attributes(Reader& r,const std::vector<ConstantPoolEntry>& cp,std::uint16_t n){
 for(std::uint16_t i=0;i<n;++i){(void)cp_utf8(cp,r.u2());r.skip(r.u4());}
}
ClassMethodInfo parse_method(Reader& r,const std::vector<ConstantPoolEntry>& cp){
 ClassMethodInfo m;m.access_flags=r.u2();m.name=cp_utf8(cp,r.u2());m.descriptor=cp_utf8(cp,r.u2());
 auto count=r.u2();for(std::uint16_t i=0;i<count;++i){
  auto attr=cp_utf8(cp,r.u2());auto length=r.u4();
  if(attr!="Code"){r.skip(length);continue;}
  if(m.has_code||length<12u)throw std::runtime_error("Malformed or duplicate Code attribute");
  m.max_stack=r.u2();m.max_locals=r.u2();auto code_length=r.u4();
  if(code_length>kMaxCodeBytes||code_length>length-12u)throw std::runtime_error("Code attribute exceeds supported bounds");
  m.code=r.byte_vector(code_length);auto exceptions=r.u2();
  r.skip(static_cast<std::size_t>(exceptions)*8u);auto nested=r.u2();skip_attributes(r,cp,nested);m.has_code=true;
 }
 return m;
}
} // namespace
ClassFileModel ClassFileModel::parse(const std::uint8_t* data,std::size_t size){
 if(size<10||size>kMaxClassBytes)throw std::runtime_error("Class file is empty, truncated, or exceeds 16 MiB");
 Reader r(data,size);if(r.u4()!=0xCAFEBABE)throw std::runtime_error("Invalid class-file magic");(void)r.u2();
 ClassFileModel m;m.major_version_=r.u2();if(m.major_version_<45||m.major_version_>70)throw std::runtime_error("Unsupported class-file version: "+std::to_string(m.major_version_));
 auto count=r.u2();if(count<1)throw std::runtime_error("Invalid constant_pool_count");m.constant_pool_.resize(count);
 for(std::uint32_t i=1;i<count;++i){auto& e=m.constant_pool_[i];e.tag=r.u1();switch(e.tag){
 case 1:e.text=r.bytes(r.u2());break;case 3:case 4:(void)r.u4();break;
 case 5:case 6:r.skip(8);if(i+1>=count)throw std::runtime_error("Invalid two-slot constant");++i;break;
 case 7:case 8:case 16:case 19:case 20:e.first=r.u2();break;
 case 9:case 10:case 11:case 12:case 17:case 18:e.first=r.u2();e.second=r.u2();break;
 case 15:e.first=r.u1();e.second=r.u2();break;
 default:throw std::runtime_error("Unsupported constant-pool tag "+std::to_string(e.tag));
 }}
 m.access_flags_=r.u2();m.name_=cp_class(m.constant_pool_,r.u2());auto super=r.u2();if(super)m.super_name_=cp_class(m.constant_pool_,super);
 auto interfaces=r.u2();for(std::uint16_t i=0;i<interfaces;++i)m.interfaces_.push_back(cp_class(m.constant_pool_,r.u2()));
 auto fields=r.u2();for(std::uint16_t i=0;i<fields;++i){ClassFieldInfo f;f.access_flags=r.u2();f.name=cp_utf8(m.constant_pool_,r.u2());f.descriptor=cp_utf8(m.constant_pool_,r.u2());skip_attributes(r,m.constant_pool_,r.u2());m.fields_.push_back(std::move(f));}
 auto methods=r.u2();for(std::uint16_t i=0;i<methods;++i)m.methods_.push_back(parse_method(r,m.constant_pool_));
 skip_attributes(r,m.constant_pool_,r.u2());if(r.remaining())throw std::runtime_error("Unexpected trailing class-file bytes");
 if(m.name_.empty()||m.name_[0]=='['||m.name_.find('.')!=std::string::npos)throw std::runtime_error("Invalid internal class name");return m;
}
const std::string& ClassFileModel::name()const{return name_;}
const std::string& ClassFileModel::super_name()const{return super_name_;}
const std::vector<std::string>& ClassFileModel::interfaces()const{return interfaces_;}
const std::vector<ClassFieldInfo>& ClassFileModel::fields()const{return fields_;}
const std::vector<ClassMethodInfo>& ClassFileModel::methods()const{return methods_;}
const std::vector<ConstantPoolEntry>& ClassFileModel::constant_pool()const{return constant_pool_;}
std::uint16_t ClassFileModel::access_flags()const{return access_flags_;}
std::uint16_t ClassFileModel::major_version()const{return major_version_;}
std::string ClassFileModel::class_name_at(std::uint16_t i)const{return cp_class(constant_pool_,i);}
ResolvedMemberReference ClassFileModel::member_reference_at(std::uint16_t i)const{
 if(!i||i>=constant_pool_.size())throw std::runtime_error("Invalid member reference constant-pool index");
 const auto& ref=constant_pool_[i];if(ref.tag!=9&&ref.tag!=10&&ref.tag!=11)throw std::runtime_error("Constant-pool entry is not a member reference");
 if(!ref.second||ref.second>=constant_pool_.size())throw std::runtime_error("Invalid NameAndType constant-pool index");
 const auto& nt=constant_pool_[ref.second];if(nt.tag!=12)throw std::runtime_error("Expected CONSTANT_NameAndType");
 return {ref.tag,class_name_at(ref.first),cp_utf8(constant_pool_,nt.first),cp_utf8(constant_pool_,nt.second)};
}
const ClassMethodInfo* ClassFileModel::find_method(const std::string& n,const std::string& d)const{
 for(const auto& m:methods_)if(m.name==n&&m.descriptor==d)return &m;return nullptr;
}
} // namespace solarcrystal
