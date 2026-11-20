#pragma once
#include "vm_value.h"
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>
namespace solarcrystal {
class VmArrayHeap {
public:
 static constexpr std::size_t MAX_ARRAYS=4096;
 static constexpr std::size_t MAX_ARRAY_LENGTH=1048576;
 ObjectId new_int_array(std::int32_t length);
 ObjectId new_reference_array(std::int32_t length,const std::string& component_class);
 std::size_t length(ObjectId id) const;
 bool is_array(ObjectId id) const;
 std::int32_t get_int(ObjectId id,std::int32_t index) const;
 void put_int(ObjectId id,std::int32_t index,std::int32_t value);
 VmValue get_reference(ObjectId id,std::int32_t index) const;
 void put_reference(ObjectId id,std::int32_t index,const VmValue& value);
 const std::string& component_class(ObjectId id) const;
private:
 struct Array { bool references=false; std::string component; std::vector<std::int32_t> ints; std::vector<VmValue> refs; };
 std::vector<Array> arrays_;
 const Array& checked(ObjectId id) const;
 Array& checked(ObjectId id);
 static ObjectId encode(std::size_t index);
 static std::size_t decode(ObjectId id);
};
}