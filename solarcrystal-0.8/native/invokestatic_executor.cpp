#include "class_method_executor.h"
#include "vm_runtime.h"
#include <cstdint>
#include <functional>
#include <map>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace solarcrystal {
namespace {
class Reader {
public:
    Reader(const std::uint8_t* p, std::size_t n):p_(p),n_(n){}
    std::uint8_t u1(){need(1);return p_[pos_++];}
    std::uint16_t u2(){return static_cast<std::uint16_t>((u1()<<8)|u1());}
    std::uint32_t u4(){return (std::uint32_t(u1())<<24)|(std::uint32_t(u1())<<16)|(std::uint32_t(u1())<<8)|u1();}
    std::string utf(std::uint16_t n){need(n);std::string s(reinterpret_cast<const char*>(p_+pos_),n);pos_+=n;return s;}
    void skip(std::size_t n){need(n);pos_+=n;}
    std::size_t remaining()const{return n_-pos_;}
    const std::uint8_t* current()const{return p_+pos_;}
private:
    const std::uint8_t* p_;std::size_t n_,pos_=0;
    void need(std::size_t n){if(n>n_-pos_)throw std::runtime_error("Truncated class file");}
};
struct Cp{std::uint8_t tag=0;std::uint16_t a=0,b=0;std::string text;};
struct Method{std::uint16_t access=0,stack=0,locals=0;std::string name,desc;std::vector<std::uint8_t> code;};
struct Ref{std::string owner,name,desc;};
struct Parsed{std::string name;std::map<std::uint16_t,Ref> refs;std::map<std::string,Method> methods;};
std::string utf(const std::vector<Cp>& cp,std::uint16_t i){if(!i||i>=cp.size()||cp[i].tag!=1)throw std::runtime_error("Invalid UTF8 constant");return cp[i].text;}
std::string class_name(const std::vector<Cp>& cp,std::uint16_t i){if(!i||i>=cp.size()||cp[i].tag!=7)throw std::runtime_error("Invalid class constant");return utf(cp,cp[i].a);}
void skip_attrs(Reader& r,const std::vector<Cp>& cp,std::uint16_t count){for(std::uint16_t i=0;i<count;++i){(void)utf(cp,r.u2());auto n=r.u4();if(n>r.remaining())throw std::runtime_error("Attribute exceeds class bounds");r.skip(n);}}
std::size_t arg_count(const std::string& d){if(d.empty()||d[0]!='(')throw std::runtime_error("Invalid invocation descriptor");std::size_t i=1,n=0;while(i<d.size()&&d[i]!=')'){if(d[i++]!='I')throw std::runtime_error("invokestatic supports integer arguments only");++n;}if(i>=d.size()||d[i]!=')'||d.substr(i+1)!="I")throw std::runtime_error("invokestatic supports integer return only");return n;}
Parsed parse(const std::uint8_t* data,std::size_t size){
    Reader r(data,size);if(r.u4()!=0xCAFEBABE)throw std::runtime_error("Invalid class-file magic");r.u2();auto major=r.u2();if(major<45||major>70)throw std::runtime_error("Unsupported class version");auto count=r.u2();if(!count)throw std::runtime_error("Invalid constant pool");std::vector<Cp> cp(count);
    for(std::uint16_t i=1;i<count;++i){auto& e=cp[i];e.tag=r.u1();switch(e.tag){case 1:e.text=r.utf(r.u2());break;case 3:case 4:r.skip(4);break;case 5:case 6:r.skip(8);if(++i>=count)throw std::runtime_error("Invalid two-slot constant");break;case 7:e.a=r.u2();break;case 8:case 16:case 19:case 20:e.a=r.u2();break;case 9:case 10:case 11:case 12:case 17:case 18:e.a=r.u2();e.b=r.u2();break;case 15:e.a=r.u1();e.b=r.u2();break;default:throw std::runtime_error("Unsupported constant-pool tag");}}
    (void)r.u2();auto this_class=r.u2();r.u2();Parsed out;out.name=class_name(cp,this_class);auto interfaces=r.u2();r.skip(std::size_t(interfaces)*2);auto fields=r.u2();for(std::uint16_t i=0;i<fields;++i){r.skip(6);skip_attrs(r,cp,r.u2());}
    auto methods=r.u2();for(std::uint16_t i=0;i<methods;++i){Method m;m.access=r.u2();m.name=utf(cp,r.u2());m.desc=utf(cp,r.u2());auto ac=r.u2();for(std::uint16_t a=0;a<ac;++a){auto an=utf(cp,r.u2());auto len=r.u4();if(len>r.remaining())throw std::runtime_error("Method attribute exceeds class bounds");if(an=="Code"){Reader cr(r.current(),len);m.stack=cr.u2();m.locals=cr.u2();auto cl=cr.u4();if(!cl||cl>65535||cl>cr.remaining())throw std::runtime_error("Invalid Code length");m.code.reserve(cl);for(std::uint32_t k=0;k<cl;++k)m.code.push_back(cr.u1());auto ex=cr.u2();if(ex)throw std::runtime_error("Exception handlers are not supported");auto nested=cr.u2();skip_attrs(cr,cp,nested);if(cr.remaining())throw std::runtime_error("Malformed Code attribute");}r.skip(len);}if(!m.code.empty())out.methods[m.name+m.desc]=m;}
    skip_attrs(r,cp,r.u2());if(r.remaining())throw std::runtime_error("Trailing class bytes");for(std::uint16_t i=1;i<count;++i)if(cp[i].tag==10){auto nt=cp[i].b;if(!nt||nt>=count||cp[nt].tag!=12)throw std::runtime_error("Invalid Methodref NameAndType");out.refs[i]={class_name(cp,cp[i].a),utf(cp,cp[nt].a),utf(cp,cp[nt].b)};}return out;
}
std::int32_t return_value(const std::string& output){auto p=output.find("Return value: ");if(output.find("SolarCrystal VM: OK")!=0||p==std::string::npos)throw std::runtime_error(output);return static_cast<std::int32_t>(std::stoll(output.substr(p+14)));}
}

std::string execute_class_method(const std::uint8_t* data,std::size_t size,const std::string& name,const std::string& desc,const std::vector<std::int32_t>& args){
try{
    if(!data||!size||size>16*1024*1024)throw std::runtime_error("Class file is empty or exceeds 16 MiB");Parsed cls=parse(data,size);auto target=cls.methods.find(name+desc);if(target==cls.methods.end())throw std::runtime_error("Requested method has no Code attribute");if(!(target->second.access&0x0008))throw std::runtime_error("Only static methods are supported");if(arg_count(desc)!=args.size())throw std::runtime_error("Argument count mismatch");
    int depth=0;std::size_t shared_steps=0;std::function<std::int32_t(const Method&,const std::vector<std::int32_t>&)> run;
    run=[&](const Method& m,const std::vector<std::int32_t>& argv)->std::int32_t{
        if(++depth>32){--depth;throw std::runtime_error("Maximum invokestatic call depth (32) exceeded");}
        try{
            if(argv.size()>m.locals)throw std::runtime_error("Arguments exceed max_locals");
            StaticCallHandler handler=[&](std::uint16_t index,std::vector<std::int32_t>& stack)->std::int32_t{
                auto ri=cls.refs.find(index);if(ri==cls.refs.end())throw std::runtime_error("invokestatic constant is not CONSTANT_Methodref");const Ref& ref=ri->second;
                if(ref.owner!=cls.name)throw std::runtime_error("Only same-class invokestatic is supported");auto mi=cls.methods.find(ref.name+ref.desc);if(mi==cls.methods.end())throw std::runtime_error("invokestatic target method not found");if(!(mi->second.access&0x0008))throw std::runtime_error("invokestatic target is not static");
                auto n=arg_count(ref.desc);if(stack.size()<n)throw std::runtime_error("Operand stack underflow at invokestatic");std::vector<std::int32_t> call_args(n);for(std::size_t x=n;x>0;--x){call_args[x-1]=stack.back();stack.pop_back();}return run(mi->second,call_args);
            };
            auto output=execute_bytecode(m.code.data(),m.code.size(),m.stack,m.locals,argv,handler,&shared_steps);auto value=return_value(output);--depth;return value;
        }catch(...){--depth;throw;}
    };
    auto value=run(target->second,args);std::ostringstream out;out<<"SolarCrystal Class VM: OK\nClass: "<<cls.name<<"\nMethod: "<<name<<desc<<"\nArguments: "<<args.size()<<"\nReturn value: "<<value<<"\nExecution: same-class invokestatic supported\nMaximum call depth: 32\nShared instruction budget: "<<shared_steps<<"/1000000";return out.str();
}catch(const std::exception& e){return std::string("SolarCrystal Class VM: ERROR\n")+e.what();}
}
}
