#pragma once
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>
namespace solarcrystal {
struct ConstantPoolEntry { std::uint8_t tag=0; std::uint16_t first=0, second=0; std::string text; };
struct ClassFieldInfo { std::uint16_t access_flags=0; std::string name, descriptor; };
struct ClassMethodInfo { std::uint16_t access_flags=0; std::string name, descriptor; bool has_code=false; std::uint16_t max_stack=0, max_locals=0; std::vector<std::uint8_t> code; };
struct ResolvedMemberReference { std::uint8_t tag=0; std::string owner, name, descriptor; };
class ClassFileModel {
public:
 static ClassFileModel parse(const std::uint8_t* data, std::size_t size);
 const std::string& name() const; const std::string& super_name() const;
 const std::vector<std::string>& interfaces() const;
 const std::vector<ClassFieldInfo>& fields() const;
 const std::vector<ClassMethodInfo>& methods() const;
 const std::vector<ConstantPoolEntry>& constant_pool() const;
 std::uint16_t access_flags() const; std::uint16_t major_version() const;
 std::string class_name_at(std::uint16_t index) const;
 ResolvedMemberReference member_reference_at(std::uint16_t index) const;
 const ClassMethodInfo* find_method(const std::string& name,const std::string& descriptor) const;
private:
 std::string name_, super_name_;
 std::vector<std::string> interfaces_;
 std::vector<ClassFieldInfo> fields_;
 std::vector<ClassMethodInfo> methods_;
 std::vector<ConstantPoolEntry> constant_pool_;
 std::uint16_t access_flags_=0, major_version_=0;
};
} // namespace solarcrystal
